package services


import models.Archetype

trait ArchetypesService {
  
  def loadFromAllCatalogs: List[Archetype]
  def findAll: List[Archetype]
  def find(groupId: Option[String], artifactId: Option[String], version: Option[String], description: Option[String], javaVersion: Option[String]): List[Archetype]
  def addAll(archetypes: List[Archetype])
  def safe(newArchetype: Archetype): Unit
  
  def loadArchetypeContent(archetype: Archetype): Archetype
  
}
