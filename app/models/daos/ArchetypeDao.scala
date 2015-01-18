package models.daos

import models.Archetype

trait ArchetypeDao {

  def safe(archetype: Archetype): Unit
  def findAll: List[Archetype]
  
}
