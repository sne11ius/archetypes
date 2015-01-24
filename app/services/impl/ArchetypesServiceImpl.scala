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
import scala.concurrent.duration.Duration
import scala.concurrent.Await
import javax.inject.Inject
import models.daos.ArchetypeDao
import org.apache.maven.artifact.versioning.ComparableVersion
import scala.sys.process._
import java.io.File
import java.io.FileInputStream
import org.apache.commons.io.IOUtils
import models.ArchetypeContent

class ArchetypesServiceImpl @Inject() (archetypsDao: ArchetypeDao) extends ArchetypesService {
  
  implicit val context = play.api.libs.concurrent.Execution.Implicits.defaultContext
  
  override def load: List[Archetype] = {
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
            None
          )
        }
      }, 10 seconds)
    }.distinct
  }
  
  override def findAll: List[Archetype] = {
    archetypsDao.findAll
  }

  def addAll(archetypes: List[Archetype]) = {
    archetypes.map { archetypsDao.safe }
  }
  
  def safe(archetype: Archetype): Unit = {
    archetypsDao.safe(archetype)
  }
  
  override def find(groupId: Option[String], artifactId: Option[String], version: Option[String], description: Option[String]): List[Archetype] = {
    // This will be slow as hell, but I cannot slick, so...
    val allArchetypes = archetypsDao.findAll
    Logger.debug(s"Total # archetypes: ${allArchetypes.size}")
    val archetypes = archetypsDao.findAll.filter { a =>
      if (groupId.isDefined) {
        a.groupId.toLowerCase().contains(groupId.get.toLowerCase())
      } else {
        true
      }
    }.filter { a =>
      if (artifactId.isDefined) {
        a.artifactId.toLowerCase().contains(artifactId.get.toLowerCase())
      } else {
        true
      }
    }.filter { a =>
      if (description.isDefined) {
        description.get.split("[\\p{Punct}\\s]+").filterNot { s => s.trim().isEmpty() }.exists { s =>
          a.description.getOrElse("").toLowerCase().contains(s.toLowerCase())
        }
      } else {
        true
      }
    }.filter { a =>
      if (version.isDefined && version.get != "newest") {
        a.version.toLowerCase().contains(version.get.toLowerCase())
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
    Logger.debug(s"Total # archetypes after filter: ${result.size}")
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
  
  private def archetypeGenerate(archetype: Archetype, groupId: String, artifactId: String, baseDir: String) = {
    Logger.debug(s"Creating $baseDir")
    val process = Process((new java.lang.ProcessBuilder(
      "mvn",
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
        "-Dmodule=ExampleModule"
    )) directory new File(baseDir))
    if (!(new File(baseDir).mkdirs())) {
      Logger.error(s"Cannot mkdir: $baseDir")
    }
    Logger.debug(s"Generating: $process")
    0 == process.!
  }
  
  override def loadArchetypeContent(archetype: Archetype): Option[ArchetypeContent] = {
    val baseDir = buildFilename(current.configuration.getString("tempDir").get, archetype)
    Logger.debug(s"baseDir: $baseDir")
    if (!archetypeGenerate(archetype, "com.example", "example-app", baseDir)) {
      Logger.debug("Cannot archetypeGenerate D:")
      None
    } else {
      Some(ArchetypeContent(archetype, baseDir + "/example-app"))
    }
  }
}
