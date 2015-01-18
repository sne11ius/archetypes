package models.daos.slick

import play.api.Play.current

import play.api.db.slick._
import play.api.db.slick.Config.driver.simple._

object ArchetypeSlickDB {
  case class DBArchetype (
    id: Option[Long],
    groupId: String,
    artifactId: String,
    version: String,
    description: Option[String],
    repository: Option[String],
    localDir: Option[String]
  )
  
  class Archetypes(tag: Tag) extends Table[DBArchetype](tag, "archetype") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def groupId = column[String]("groupId")
    def artifactId = column[String]("artifactId")
    def version = column[String]("version")
    def description = column[Option[String]]("description", O.DBType("LONGTEXT"))
    def repository = column[Option[String]]("repository")
    def localDir = column[Option[String]]("localDir")
    def * = (id.?, groupId, artifactId, version, description, repository, localDir) <> (DBArchetype.tupled, DBArchetype.unapply)
    
    def idx = index("idx_a", (groupId, artifactId, version), unique = true)
  }
  
  val archetypes = TableQuery[Archetypes]
}
