package models.daos.slick

import models.Archetype
import models.daos.ArchetypeDao
import play.api.db.slick._
import play.api.db.slick.Config.driver.simple._
import models.daos.slick.ArchetypeSlickDB._
import play.api.Play.current
import play.api.Logger
import org.apache.maven.artifact.versioning.ComparableVersion
import models.Archetype

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
  
  def find(groupId: Option[String], artifactId: Option[String], version: Option[String]): List[Archetype] = {
    // This will be slow as hell, but I cannot slick, so...
    val archetypes = findAll.filter { a =>
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
