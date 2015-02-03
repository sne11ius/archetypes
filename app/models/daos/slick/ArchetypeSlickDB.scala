package models.daos.slick

import play.api.Play.current
import play.api.db.slick._
import play.api.db.slick.Config.driver.simple._
import org.joda.time.DateTime


object ArchetypeSlickDB {
  
  implicit val stringListMapper = MappedColumnType.base[List[String],String](
    list => list.mkString(","),
    string => string.split(',').toList
  )
  
  case class DBArchetype (
    id: Option[Long],
    groupId: String,
    artifactId: String,
    version: String,
    description: Option[String],
    repository: Option[String],
    javaVersion: Option[String],
    packaging: Option[String],
    lastUpdated: Long,
    localDir: Option[String],
    generateLog: Option[String],
    additionalProps: List[String]
  )
  
  class Archetypes(tag: Tag) extends Table[DBArchetype](tag, "archetype") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def groupId = column[String]("groupId")
    def artifactId = column[String]("artifactId")
    def version = column[String]("version")
    def description = column[Option[String]]("description", O.DBType("LONGTEXT"))
    def repository = column[Option[String]]("repository")
    def javaVersion = column[Option[String]]("javaVersion")
    def packaging = column[Option[String]]("packaging")
    def lastUpdated = column[Long]("lastUpdated")
    def localDir = column[Option[String]]("localDir")
    def generateLog = column[Option[String]]("generateLog", O.DBType("LONGTEXT"))
    def additionalProps = column[List[String]]("additionalProps", O.DBType("LONGTEXT"))
    def * = (id.?, groupId, artifactId, version, description, repository, javaVersion, packaging, lastUpdated, localDir, generateLog, additionalProps) <> (DBArchetype.tupled, DBArchetype.unapply)
    
    def idx = index("idx_a", (groupId, artifactId, version), unique = true)
  }
  
  val archetypes = TableQuery[Archetypes]
}
