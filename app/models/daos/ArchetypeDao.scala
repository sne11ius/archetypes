package models.daos

import models.Archetype

trait ArchetypeDao {

  def safe(archetype: Archetype)
  def findAll: List[Archetype]
  def find(groupId: Option[String], artifactId: Option[String], version: Option[String]): List[Archetype]
  
}
