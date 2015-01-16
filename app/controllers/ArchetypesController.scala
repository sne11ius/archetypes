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
  
  implicit val archetypeWrites = Json.writes[Archetype]

  def restArchetypes(groupId: Option[String], artifactId: Option[String], version: Option[String], description: Option[String]) = DBAction { implicit rs =>
    Ok(Json.toJson(archetypesService.find(groupId, artifactId, version, description)))
  }
  
}
