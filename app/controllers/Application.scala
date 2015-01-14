package controllers

import play.api._
import play.api.mvc._
import javax.inject.Inject
import services.ArchetypesService
import play.api.db.slick.DBAction

class Application @Inject() (archetypesService: ArchetypesService) extends Controller {

  def index = DBAction { implicit rs =>
    Ok(views.html.index(archetypesService.findAll))
  }

}
