package services.impl

import java.io.File
import java.util.Locale

import scala.collection.JavaConversions.asScalaBuffer
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.language.implicitConversions
import scala.language.postfixOps
import scala.sys.process.BasicIO
import scala.sys.process.Process
import scala.xml.XML

import org.apache.maven.artifact.versioning.ComparableVersion

import extensions.MyScalaExtensions.ExtendedNodeSeq
import javax.inject.Inject
import models.Archetype
import models.MavenGenerateResult
import models.daos.ArchetypeDao
import play.api.Logger
import play.api.Play.current
import play.api.libs.ws.WS
import services.ArchetypesService

class ArchetypesServiceImpl @Inject() (archetypsDao: ArchetypeDao) extends ArchetypesService {
  
  implicit val context = play.api.libs.concurrent.Execution.Implicits.defaultContext
  
  private def rootDir: String = {
    val osString = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH);
    val WinMatcher = "win"r
    val result = osString match {
      case WinMatcher(_) => current.configuration.getString("tempDirWindows").get
      case _ => current.configuration.getString("tempDir").get
    }
    //Logger.debug(s"Base directory: $result")
    result
  }
  
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
  
  override def find(groupId: Option[String], artifactId: Option[String], version: Option[String], description: Option[String], javaVersion: Option[String]): List[Archetype] = {
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
    }.filter { a =>
      if (javaVersion.isDefined) {
        a.javaVersion == javaVersion
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
  
    private def archetypeGenerate(archetype: Archetype, groupId: String, artifactId: String, baseDir: String): MavenGenerateResult = {
    Logger.debug(s"Creating $baseDir")
    new File(baseDir).delete()
    if (!(new File(baseDir).mkdirs())) {
      Logger.error(s"Cannot mkdir: $baseDir")
      MavenGenerateResult(-1, s"Cannot create base directory: $baseDir")
    } else {
      val sb = new StringBuffer
      val process = Process((new java.lang.ProcessBuilder(
        //"C:\\Users\\coli\\Documents\\bin\\apache-maven-3.0.5\\bin\\mvn.bat",
        "mvn",
        "archetype:generate",
        "-DinteractiveMode=false",
        s"-DarchetypeGroupId=${archetype.groupId}",
        s"-DarchetypeArtifactId=${archetype.artifactId}",
        s"-DarchetypeVersion=${archetype.version}",
        s"-DgroupId=$groupId",
        s"-DartifactId=$artifactId",
        "-DprojectName=ExampleProject",
        //"-projectDescription=ExampleDescription",
        "-DnewProjectName=ExampleProject",
        "-DmoduleName=ExampleModule",
        "-Dmodule=ExampleModule",
        "-DwebContextPath=TestWebContextPath",
        "-DgaeApplicationName=TestGaeApplicationName"
      )) directory new File(baseDir))
      val exitValue = process.run(BasicIO(false, sb, None)).exitValue
      MavenGenerateResult(exitValue, sb.toString.replace(baseDir, "").replace("\\", "/"))
    }
  }
  
  override def loadArchetypeContent(archetype: Archetype): Archetype = {
    val baseDir = buildFilename(rootDir, archetype)
    if (archetype.localDir.isDefined) {
      Logger.debug(s"localDir already defined as: ${archetype.localDir}")
      archetype
    } else {
      Logger.debug(s"baseDir: $baseDir")
      archetypeGenerate(archetype, "com.example", "example-app", baseDir) match {
        case MavenGenerateResult(0, stdout) => {
          archetype.copy(
            javaVersion = Some(extractJavaVersion(baseDir)),
            localDir = Some(baseDir),
            generateLog = Some(stdout)
          )
        }
        case MavenGenerateResult(_, stdout) => {
          archetype.copy(
              generateLog = Some(stdout)
          )
        }
      }
    }
  }
  
  private def extractJavaVersion(baseDir: String): String = {
    val file = new File(new File(baseDir, "example-app"), "pom.xml")
    try {
      val xml = XML.loadFile(file)
      var javaVersion = ((xml \ "build" \ "plugins" \ "plugin" \ "configuration" \ "source") text)
      if (javaVersion.contains("$")) {
        Logger.debug(s"We need to go deeper for $file")
        val property = javaVersion.drop(2).dropRight(1)
        javaVersion = ((xml \ "properties" \ property) text)
      }
      Logger.debug(s"Java version: $javaVersion")
      if ("" == javaVersion) {
        javaVersion = ((xml \ "properties" \ "maven.compiler.source") text)
      }
      if ("" == javaVersion) {
        "[default]"
      } else {
        javaVersion
      }
    } catch {
      case e: Exception => {
        Logger.debug(s"Cannot parse $file: ${e getMessage}")
        "[default]"
      }
    }
  }
}
