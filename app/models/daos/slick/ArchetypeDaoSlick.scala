package models.daos.slick

import models.Archetype
import models.daos.ArchetypeDao
import play.api.db.slick._
import play.api.db.slick.Config.driver.simple._
import models.daos.slick.ArchetypeSlickDB._
import play.api.Play.current
import play.api.Logger

class ArchetypeDaoSlick extends ArchetypeDao {
  
  def safe(archetype: Archetype): Unit = {
    DB withSession { implicit session =>
      archetypes.filter(a => a.groupId === archetype.groupId && a.artifactId === archetype.artifactId && a.version === archetype.version).firstOption match {
        case None => archetypes.insert(DBArchetype(
          None, archetype.groupId, archetype.artifactId, archetype.version, archetype.description, archetype.repository
        ))
        case Some(_) => ()
      }
    }
  }

  def findAll: List[Archetype] = {
    DB withSession { implicit session =>
      archetypes.list.sortBy { a => a.groupId }.map { a => Archetype(
        a.id, a.groupId, a.artifactId, a.version, a.description, a.repository
      )}
    }
  }
  
  def find(groupId: Option[String]): List[Archetype] = {
    Logger.debug(s"Filter for $groupId")
    DB withSession { implicit session =>
      if (groupId.isEmpty) {
        Logger.debug(s"Finding all...")
        findAll
      }
      archetypes.filter { _.groupId === groupId.get }.list.map { a => Archetype(
        a.id, a.groupId, a.artifactId, a.version, a.description, a.repository
      )}
    }
  }
  
}
