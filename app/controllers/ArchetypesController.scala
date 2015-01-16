package controllers

import play.api._
import play.api.mvc._
import javax.inject.Inject
import services.ArchetypesService
import play.api.db.slick.DBAction

class ArchetypesController @Inject() (archetypesService: ArchetypesService) extends Controller {
  
  def reimportArchetypes = DBAction { implicit rs =>
    var newArchetypes = archetypesService.load
    archetypesService.addAll(newArchetypes)
    Ok(views.html.index(archetypesService.findAll))
  }
  
  def archetypes(groupId: Option[String], artifactId: Option[String], version: Option[String]) = DBAction { implicit rs =>
    val archetypes = archetypesService.find(groupId, artifactId, version);
    Ok(views.html.index(archetypes))
  }
  
}
