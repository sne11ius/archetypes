package services


import models.Archetype;

trait ArchetypesService {
  def loadArchetypes : Set[Archetype]
}
