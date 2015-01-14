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

class ArchetypesServiceImpl @Inject() (archetypsDao: ArchetypeDao) extends ArchetypesService {
  
  implicit val context = play.api.libs.concurrent.Execution.Implicits.defaultContext
  
  override def loadArchetypes : List[Archetype] = {
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

}
