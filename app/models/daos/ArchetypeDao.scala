package models.daos

import models.Archetype

trait ArchetypeDao {

  def safe(archetype: Archetype)
  def findBy(ex: Archetype): Option[Archetype]
  def findAll: List[Archetype]
  
}
