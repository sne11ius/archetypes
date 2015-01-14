package models.daos

import models.Archetype

trait ArchetypeDao {

  def safe(archetype: Archetype);
  
  def findAll: List[Archetype]
  
}
