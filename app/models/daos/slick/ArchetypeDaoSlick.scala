package models.daos.slick

import models.Archetype
import models.daos.ArchetypeDao
import play.api.db.slick._
import play.api.db.slick.Config.driver.simple._
import models.daos.slick.ArchetypeSlickDB._
import play.api.Play.current
import play.api.Logger
import models.Archetype

class ArchetypeDaoSlick extends ArchetypeDao {
  
  def safe(a: Archetype): Unit = {
    DB withSession { implicit session =>
      val dbArchetype = DBArchetype(a.id, a.groupId, a.artifactId, a.version, a.description, a.repository, a.localDir)
      archetypes.filter(_.id === dbArchetype.id).firstOption match {
        case Some(archetypeFound) => archetypes.filter(_.id === dbArchetype.id).update(dbArchetype)
        case None => archetypes.insert(dbArchetype)
      }
      /*
      archetypes.filter(a => a.groupId === archetype.groupId && a.artifactId === archetype.artifactId && a.version === archetype.version).firstOption match {
        case None => archetypes.insert(DBArchetype(
          None, archetype.groupId, archetype.artifactId, archetype.version, archetype.description, archetype.repository, archetype.localDir
        ))
        case Some(_) => ()
      }*/
    }
  }

  def findAll: List[Archetype] = {
    DB withSession { implicit session =>
      archetypes.list.sortBy { a => a.groupId }.map { a => Archetype(
        a.id, a.groupId, a.artifactId, a.version, a.description, a.repository, a.localDir
      )}
    }
  }
  
}
