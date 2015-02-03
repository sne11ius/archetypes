package models.daos.slick

import models.Archetype
import models.daos.ArchetypeDao
import play.api.db.slick._
import play.api.db.slick.Config.driver.simple._
import models.daos.slick.ArchetypeSlickDB._
import play.api.Play.current
import play.api.Logger
import models.Archetype
import org.joda.time.DateTime

class ArchetypeDaoSlick extends ArchetypeDao {
  
  override def safe(a: Archetype) = {
    DB withSession { implicit session =>
      val dbArchetype = DBArchetype(
        a.id,
        a.groupId,
        a.artifactId,
        a.version,
        a.description,
        a.repository,
        a.javaVersion,
        a.packaging,
        a.lastUpdated.getMillis,
        a.localDir,
        a.generateLog,
        a.additionalProps
      )
      archetypes.filter(_.id === dbArchetype.id).firstOption match {
        case Some(archetypeFound) => archetypes.filter(_.id === dbArchetype.id).update(dbArchetype)
        case None => {
          archetypes.filter(dba => { dba.groupId === a.groupId && dba.artifactId === a.artifactId && dba.version === a.version}).firstOption match {
            case None => archetypes.insert(dbArchetype)
            case Some(archetypeFound) => archetypes.filter(_.id === dbArchetype.id).update(dbArchetype)
          }
        }
      }
    }
  }
  
  override def findBy(ex: Archetype): Option[Archetype] = {
    DB withSession { implicit session =>
      archetypes.filter( a => {a.groupId === ex.groupId && a.artifactId === ex.artifactId && a.version === ex.version}).firstOption match {
        case Some(found) => {
          Some(Archetype(
            found.id,
            found.groupId,
            found.artifactId,
            found.version,
            found.description,
            found.repository,
            found.javaVersion,
            found.packaging,
            new DateTime(found.lastUpdated),
            found.localDir,
            found.generateLog,
            found.additionalProps
          ))
        }
        case None => {
          None
        }
      }
    }
  }

  override def findAll: List[Archetype] = {
    DB withSession { implicit session =>
      archetypes.list.sortBy { a => a.groupId }.map { a => Archetype(
        a.id,
        a.groupId,
        a.artifactId,
        a.version,
        a.description,
        a.repository,
        a.javaVersion,
        a.packaging,
        new DateTime(a.lastUpdated),
        a.localDir,
        a.generateLog,
        a.additionalProps
      )}
    }
  }
  
}
