package services

import models.Archetype
import java.io.File

trait SourcePrettifyService {
  
  def toPrettyHtml(baseDir: File, file: String): String
  
}
