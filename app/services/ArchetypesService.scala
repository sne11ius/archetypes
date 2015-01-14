package services


import models.Archetype;

trait ArchetypesService {
  
  def loadArchetypes: List[Archetype]
  def findAll: List[Archetype]
  
}
