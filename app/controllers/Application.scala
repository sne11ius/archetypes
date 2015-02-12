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
import models.ManifestInfo
import com.jcabi.manifests.Manifests

class Application @Inject() (archetypesService: ArchetypesService) extends Controller {

  def untrail(path: String) = Action {
    Redirect("/" + path)
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
