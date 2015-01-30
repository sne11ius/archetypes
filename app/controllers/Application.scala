package controllers

import play.api._
import play.api.mvc._
import javax.inject.Inject
import services.ArchetypesService
import play.api.db.slick.DBAction
import views.forms.search.ArchetypeSearch._
import play.api.data._
import play.api.data.Forms._
import models.PaginationInfo

class Application @Inject() (archetypesService: ArchetypesService) extends Controller {

  def untrail(path: String) = Action {
    Redirect("/" + path)
  }
  
  def index() = DBAction { implicit rs =>
    archetypeSearchForm.bindFromRequest.fold(
      formWithErrors => {
        val searchData = SearchData(None, None, None, None, None)
        BadRequest(views.html.index(formWithErrors, List(), None, searchData, 0))
      },
      searchData => {
        Logger.debug(s"Search data: $searchData")
        val start = Form("start" -> text).bindFromRequest.fold( hasErrors => { 0 }, value => { value.toInt } )
        val numItems = Form("numItems" -> text).bindFromRequest.fold( hasErrors => { 200 }, value => { value.toInt } )
        val archetypes = archetypesService.find(searchData.groupId, searchData.artifactId, searchData.version, searchData.description, searchData.javaVersion)
        val numArchetypes = archetypes.length
        val numPages = ((numArchetypes:Float) / numItems).ceil.toInt
        val paginationInfo = PaginationInfo(start, numItems, numPages)
        Logger.debug(s"paginationInfo: $paginationInfo")
        Ok(views.html.index(archetypeSearchForm.fill(searchData), archetypes.drop(start).take(numItems), Some(paginationInfo), searchData, numArchetypes))
      }
    )
  }

}
