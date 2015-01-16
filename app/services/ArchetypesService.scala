package services


import models.Archetype;
import models.Archetype

trait ArchetypesService {
  
  def load: List[Archetype]
  def findAll: List[Archetype]
  def find(groupId: Option[String], artifactId: Option[String], version: Option[String]): List[Archetype]
  def addAll(archetypes: List[Archetype])
  
}
