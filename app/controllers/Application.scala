package controllers

import javax.inject.Inject

import com.jcabi.manifests.Manifests
import models.{ManifestInfo, PaginationInfo}
import play.api._
import play.api.data.Forms._
import play.api.data._
import play.api.db.slick.DBAction
import play.api.mvc._
import services.ArchetypesService
import views.forms.search.ArchetypeSearch._

class Application @Inject() (archetypesService: ArchetypesService) extends Controller {

  def untrail(path: String) = Action {
    Redirect("/" + path)
  }
  
  def about() = Action { implicit rs =>
    var manifestInfo = ManifestInfo("branch", "date", "rev")
    try {
      manifestInfo = ManifestInfo(
        Manifests.read("Git-Branch"),
        Manifests.read("Git-Build-Date"),
        Manifests.read("Git-Head-Rev")
      )
    } catch {
      case e: Exception => {}
    }
    Ok(views.html.about(manifestInfo))
  }
  
  def index() = DBAction { implicit rs =>
    var manifestInfo = ManifestInfo("branch", "date", "rev")
    try {
      manifestInfo = ManifestInfo(
        Manifests.read("Git-Branch"),
        Manifests.read("Git-Build-Date"),
        Manifests.read("Git-Head-Rev")
      )
    } catch {
      case e: Exception => {}
    }
    archetypeSearchForm.bindFromRequest.fold(
      formWithErrors => {
        val searchData = SearchData(None, None, None, None, None)
        BadRequest(views.html.index(manifestInfo, formWithErrors, List(), None, searchData, 0))
      },
      searchData => {
        Logger.debug(s"Search data: $searchData")
        val start = Form("start" -> text).bindFromRequest.fold( hasErrors => { 0 }, value => { value.toInt } )
        val numItems = Form("numItems" -> text).bindFromRequest.fold( hasErrors => { 200 }, value => { value.toInt } )
        val archetypes = archetypesService.find(searchData.groupId, searchData.artifactId, Some(searchData.version.getOrElse("newest")), searchData.description, searchData.javaVersion)
        val numArchetypes = archetypes.length
        val numPages = ((numArchetypes:Float) / numItems).ceil.toInt
        val paginationInfo = PaginationInfo(start, numItems, numPages)
        Ok(views.html.index(manifestInfo, archetypeSearchForm.fill(searchData), archetypes.drop(start).take(numItems), Some(paginationInfo), searchData, numArchetypes))
      }
    )
  }

}
