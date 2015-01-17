package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import javax.inject.Inject
import services.ArchetypesService
import play.api.db.slick.DBAction
import play.api.libs.json.Json
import models.Archetype

class ArchetypesController @Inject() (archetypesService: ArchetypesService) extends Controller {
  
  def reimportArchetypes = DBAction { implicit rs =>
    var newArchetypes = archetypesService.load
    archetypesService.addAll(newArchetypes)
    Ok(views.html.index(archetypesService.findAll))
  }
  
  def archetypes(groupId: Option[String], artifactId: Option[String], version: Option[String], description: Option[String]) = DBAction { implicit rs =>
    val archetypes = archetypesService.find(groupId, artifactId, version, description)
    Ok(views.html.index(archetypes))
  }
  
  def archetypeDetails(groupId: String, artifactId: String, version: String) = DBAction { implicit rs =>
    val archetype = archetypesService.find(Some(groupId), Some(artifactId), Some(version), None);
    if (1 == archetype.size) {
      Ok(views.html.archetypeDetails(archetype.head))
    } else {
      NotFound
    }
  }
  
  implicit val archetypeWrites = Json.writes[Archetype]

  def restArchetypes(groupId: Option[String], artifactId: Option[String], version: Option[String], description: Option[String]) = DBAction { implicit rs =>
    Ok(Json.toJson(archetypesService.find(groupId, artifactId, version, description)))
  }
  
}
