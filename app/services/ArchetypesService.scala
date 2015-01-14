package services


import models.Archetype;
import models.Archetype

trait ArchetypesService {
  
  def load: List[Archetype]
  def findAll: List[Archetype]
  def addAll(archetypes: List[Archetype])
  
}
