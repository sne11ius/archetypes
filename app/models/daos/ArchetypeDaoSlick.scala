package models.daos

import play.api.db.slick._
import play.api.db.slick.Config.driver.simple._

import play.api.Play.current

import models.Archetype
import models.ArchetypeDao

class ArchetypeDaoSlick extends ArchetypeDao {
  
  class Archetypes(tag: Tag) extends Table[Archetype](tag, "archetype") {
    def groupId = column[String]("groupId")
    def artifactId = column[String]("artifactId")
    def version = column[String]("version")
    def description = column[Option[String]]("description", O.DBType("LONGTEXT"))
    def repository = column[Option[String]]("repository")
    def * = (groupId, artifactId, version, description, repository) <> (Archetype.tupled, Archetype.unapply)
    
    def pk = primaryKey("pk_a", (groupId, artifactId, version))
  }
  
  val archetypes = TableQuery[Archetypes]
  
//  DB withSession { implicit session =>
//    archetypes.ddl.create
//  }
//  slickLoginInfos.filter(info => info.providerID === dbLoginInfo.providerID && info.providerKey === dbLoginInfo.providerKey).firstOption match {
//    case None => slickLoginInfos.insert(dbLoginInfo)
//    case Some(info) => ()//Logger.debug("Nothing to insert since info already exists: " + info)
//  }
  
  def safe(archetype: Archetype): Unit = {
    DB withSession { implicit session =>
      archetypes.filter(a => a.groupId === archetype.groupId && a.artifactId === archetype.artifactId && a.version === archetype.version).firstOption match {
        case None => archetypes.insert(archetype)
        case Some(_) => ()
      }
//      archetypes.insert(archetype)
    }
  }

  def findAll: List[Archetype] = {
    DB withSession { implicit session =>
      archetypes.list.sortBy { a => a.groupId }
    }
  }
}
