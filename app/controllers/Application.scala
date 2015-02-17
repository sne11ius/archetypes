package controllers

import javax.inject.Inject
import com.jcabi.manifests.Manifests
import models.{ManifestInfo, PaginationInfo}
import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.db.slick.DBAction
import services.ArchetypesService
import views.forms.search.ArchetypeSearch._
import com.mohiva.play.silhouette.core.Environment
import models.User
import com.mohiva.play.silhouette.contrib.services.CachedCookieAuthenticator
import com.mohiva.play.silhouette.core.Silhouette

class Application @Inject() (
    archetypesService: ArchetypesService,
    implicit val env: Environment[User, CachedCookieAuthenticator]
  ) extends Controller with Silhouette[User, CachedCookieAuthenticator] {

  def untrail(path: String) = Action {
    Redirect("/" + path)
  }
  
  def about() = UserAwareAction { implicit request =>
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
    Ok(views.html.about(manifestInfo, request.identity))
  }
  
  def index() = UserAwareAction { implicit request =>
    val recentArchetyps = archetypesService.recent(10)
    archetypeSearchForm.bindFromRequest.fold(
      formWithErrors => {
        val searchData = SearchData(None, None, None, None, None)
        BadRequest(views.html.index(recentArchetyps, formWithErrors, List(), None, searchData, 0, request.identity))
      },
      searchData => {
        val start = Form("start" -> text).bindFromRequest.fold( hasErrors => { 0 }, value => { value.toInt } )
        val numItems = Form("numItems" -> text).bindFromRequest.fold( hasErrors => { 200 }, value => { value.toInt } )
        val archetypes = archetypesService.find(searchData.groupId, searchData.artifactId, Some(searchData.version.getOrElse("newest")), searchData.description, searchData.javaVersion)
        val numArchetypes = archetypes.length
        val numPages = ((numArchetypes.toFloat) / numItems).ceil.toInt
        val paginationInfo = PaginationInfo(start, numItems, numPages)
        Ok(views.html.index(recentArchetyps, archetypeSearchForm.fill(searchData), archetypes.drop(start).take(numItems), Some(paginationInfo), searchData, numArchetypes, request.identity))
      }
    )
  }

}
