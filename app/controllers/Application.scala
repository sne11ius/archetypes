package controllers

import play.api._
import play.api.mvc._
import javax.inject.Inject
import services.ArchetypesService
import models.ArchetypeDao
import play.api.db.slick.DBAction

class Application @Inject() (archetypesService: ArchetypesService, archetypeDao: ArchetypeDao) extends Controller {

  def index = DBAction { implicit rs =>
    val archetypes = archetypesService.loadArchetypes
    //archetypes.foreach { a => archetypeDao.safe(a) }
    //Ok(views.html.index(archetypesService.loadArchetypes))
    Ok(views.html.index(archetypeDao.findAll))
  }

}
