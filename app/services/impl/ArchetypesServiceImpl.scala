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

class ArchetypesServiceImpl @Inject() (archetypsDao: ArchetypeDao) extends ArchetypesService {
  
  implicit val context = play.api.libs.concurrent.Execution.Implicits.defaultContext
  
  override def load : List[Archetype] = {
    current.configuration.getStringList("archetypes.catalogs").map(_.toList).get.flatMap { url =>
      Await.result(WS.url(url).withFollowRedirects(true).get().map { response =>
        response.xml \\ "archetype-catalog" \\ "archetypes" \\ "archetype" map { a =>
          Archetype(
            None,
            (a \ "groupId").text,
            (a \ "artifactId").text,
            (a \ "version").text,
            (a \ "description").textOption,
            (a \ "repository").textOption
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
  
  override def find(groupId: Option[String], artifactId: Option[String], version: Option[String]): List[Archetype] = {
    // This will be slow as hell, but I cannot slick, so...
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
      if (version.isDefined && version.get != "newest") {
        a.version.toLowerCase().contains(version.get.toLowerCase())
      } else {
        true
      }
    }
    if (version.isDefined && version.get == "newest") {
      archetypes.groupBy( a => (a.groupId, a.artifactId)).flatMap {
        case ((groupId, artifactId), list) => {
          list.sortWith((a1, a2) => {
            0 < a1.compareTo(a2)
          }).take(1)
        }
      }.toList
    } else {
      archetypes
    }
  }
  
  implicit def archetypeToComparableVersion(a: Archetype) : ComparableVersion = new ComparableVersion(a.version)
}
