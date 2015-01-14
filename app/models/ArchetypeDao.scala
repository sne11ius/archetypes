package models

import models.Archetype
import models.Archetype

trait ArchetypeDao {

  def safe(archetype: Archetype);
  
  def findAll: List[Archetype]
  
}
