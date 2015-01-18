package services


import models.Archetype
import models.ArchetypeContent

trait ArchetypesService {
  
  def load: List[Archetype]
  def findAll: List[Archetype]
  def find(groupId: Option[String], artifactId: Option[String], version: Option[String], description: Option[String]): List[Archetype]
  def addAll(archetypes: List[Archetype])
  def safe(newArchetype: Archetype): Unit
  
  def loadArchetypeContent(archetype: Archetype): Option[ArchetypeContent]
  
}
