package services.impl

import play.api.Play.current
import extensions.MyScalaExtensions._
import scala.concurrent.duration._
import collection.JavaConversions._
import play.api.Logger
import services.ArchetypesService
import play.api.libs.ws.WS
import models.Archetype
import scala.concurrent.Future
import scala.concurrent.Await
import javax.inject.Inject
import models.daos.ArchetypeDao
import org.apache.maven.artifact.versioning.ComparableVersion
import scala.sys.process._
import java.io.File
import java.io.FileInputStream
import org.apache.commons.io.IOUtils
import models.Archetype
import models.Archetype
import models.MavenGenerateResult
import java.io.ByteArrayOutputStream
import java.io.PrintWriter
import models.MavenGenerateResult
import models.MavenGenerateResult

class ArchetypesServiceImpl @Inject() (archetypsDao: ArchetypeDao) extends ArchetypesService {
  
  implicit val context = play.api.libs.concurrent.Execution.Implicits.defaultContext
  
  override def loadFromAllCatalogs: List[Archetype] = {
    current.configuration.getStringList("archetypes.catalogs").map(_.toList).get.flatMap { url =>
      Await.result(WS.url(url).withFollowRedirects(true).get().map { response =>
        response.xml \\ "archetype-catalog" \\ "archetypes" \\ "archetype" map { a =>
          Archetype(
            None,
            (a \ "groupId").text,
            (a \ "artifactId").text,
            (a \ "version").text,
            (a \ "description").textOption,
            (a \ "repository").textOption,
            None,
            None,
            None
          )
        }
      }, 10 seconds)
    }.distinct
  }
  
  override def findAll: List[Archetype] = {
    archetypsDao.findAll
  }

  override def addAll(archetypes: List[Archetype]) = {
    archetypes.map { archetypsDao.safe }
  }
  
  override def safe(archetype: Archetype): Unit = {
    archetypsDao.safe(archetype)
  }
  
  override def find(groupId: Option[String], artifactId: Option[String], version: Option[String], description: Option[String]): List[Archetype] = {
    // This will be slow as hell, but I cannot slick, so...
    val allArchetypes = archetypsDao.findAll
    //Logger.debug(s"Total # archetypes: ${allArchetypes.size}")
    val archetypes = archetypsDao.findAll.filter { a =>
      if (groupId.isDefined) {
        a.groupId.toLowerCase().contains(groupId.get.toLowerCase)
      } else {
        true
      }
    }.filter { a =>
      if (artifactId.isDefined) {
        a.artifactId.toLowerCase().contains(artifactId.get.toLowerCase)
      } else {
        true
      }
    }.filter { a =>
      if (description.isDefined) {
        description.get.split("[\\p{Punct}\\s]+").filterNot { s => s.trim().isEmpty() }.exists { s =>
          a.description.getOrElse("").toLowerCase().contains(s.toLowerCase)
        }
      } else {
        true
      }
    }.filter { a =>
      if (version.isDefined && version.get != "newest") {
        a.version.toLowerCase().contains(version.get.toLowerCase)
      } else {
        true
      }
    }
    val result = 
      if (version.isDefined && version.get == "newest") {
        archetypes.groupBy( a => (a.groupId, a.artifactId)).flatMap {
          case ((groupId, artifactId), list) => {
            list.sortWith((a1, a2) => { 0 < a1.compareTo(a2) }).take(1)
          }
        }.toList
      } else {
        archetypes
      }
    //Logger.debug(s"Total # archetypes after filter: ${result.size}")
    result
  }
  
  implicit def archetypeToComparableVersion(a: Archetype) : ComparableVersion = new ComparableVersion(a.version)
  
  private def buildFilename(prefix: String, archetype: Archetype): String = {
    List(
      prefix,
      archetype.groupId.replace(".", File.separator),
      archetype.artifactId,
      archetype.version
    ).mkString(File.separator)
  }
  /*
  http://www.wenda.io/questions/151281/scala-process-capture-standard-out-and-exit-code.html
    private def runCommand(cmd: Seq[String]): (Int, String, String) = {
      val stdout = new ByteArrayOutputStream
      val stderr = new ByteArrayOutputStream
      val stdoutWriter = new PrintWriter(stdout)
      val stderrWriter = new PrintWriter(stderr)
      val exitValue = cmd.!(ProcessLogger(stdoutWriter.println, stderrWriter.println))
      stdoutWriter.close()
      stderrWriter.close()
      (exitValue, stdout.toString, stderr.toString)
    }
  */
  private def archetypeGenerate(archetype: Archetype, groupId: String, artifactId: String, baseDir: String): MavenGenerateResult = {
    Logger.debug(s"Creating $baseDir")
    if (!(new File(baseDir).mkdirs())) {
      Logger.error(s"Cannot mkdir: $baseDir")
      MavenGenerateResult(-1, s"Cannot create base directory: $baseDir")
    } else {
      val sb = new StringBuffer
      val process = Process((new java.lang.ProcessBuilder(
        "C:\\Users\\coli\\Documents\\bin\\apache-maven-3.0.5\\bin\\mvn.bat",
        "archetype:generate",
          "-DinteractiveMode=false",
          s"-DarchetypeGroupId=${archetype.groupId}",
          s"-DarchetypeArtifactId=${archetype.artifactId}",
          s"-DarchetypeVersion=${archetype.version}",
          s"-DgroupId=$groupId",
          s"-DartifactId=$artifactId",
          "-DprojectName=ExampleProject",
          "-DnewProjectName=ExampleProject",
          "-DmoduleName=ExampleModule",
          "-Dmodule=ExampleModule",
          "-DwebContextPath=TestWebContextPath",
          "-DgaeApplicationName=TestGaeApplicationName"
      )) directory new File(baseDir))
      val exitValue = process.run(BasicIO(false, sb, None)).exitValue
      MavenGenerateResult(exitValue, sb.toString)
    }
  }
  
  override def loadArchetypeContent(archetype: Archetype): Archetype = {
    val baseDir = buildFilename(current.configuration.getString("tempDir").get, archetype)
    if (archetype.localDir.isDefined) {
      Logger.debug(s"localDir already defined as: ${archetype.localDir}")
      archetype
    } else {
      Logger.debug(s"baseDir: $baseDir")
      archetypeGenerate(archetype, "com.example", "example-app", baseDir) match {
        case MavenGenerateResult(0, stdout) => {
          Archetype(
            archetype.id,
            archetype.groupId,
            archetype.artifactId,
            archetype.version,
            archetype.description,
            archetype.repository,
            Some(extractJavaVersion(baseDir)),
            Some(baseDir),
            Some(stdout)
          )
        }
        case MavenGenerateResult(_, stdout) => {
          Archetype(
            archetype.id,
            archetype.groupId,
            archetype.artifactId,
            archetype.version,
            archetype.description,
            archetype.repository,
            None,
            None,
            Some(stdout)
          )
        }
      }
      //Logger.debug(s"Fully loaded archetype: $loadedArchetype")
      //safe(loadedArchetype)
      //loadedArchetype
    }
  }
  
  private def extractJavaVersion(baseDir: String): String = {
    "1.4"
  }
}
