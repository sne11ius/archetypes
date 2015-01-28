package controllers

import play.api.Play.current
import play.api._
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import javax.inject.Inject
import services.ArchetypesService
import play.api.db.slick.DBAction
import play.api.libs.json.Json
import models.Archetype
import play.api.data._
import play.api.data.Forms._
import java.io.File
import java.util.Arrays
import org.apache.commons.io.IOUtils
import java.io.FileInputStream
import views.forms.search.ArchetypeSearch._
import nu.wasis.dir2html.DirToHtml
import services.SourcePrettifyService
import models.Archetype
import models.Archetype

class ArchetypesController @Inject() (archetypesService: ArchetypesService, sourcePrettifyService: SourcePrettifyService) extends Controller {
  
  def reimportArchetypes = DBAction { implicit rs =>
    var newArchetypes = archetypesService.loadFromAllCatalogs
    Logger.debug(s"${newArchetypes.size} archetypes loaded.")
    Logger.debug("Adding to database...")
    archetypesService.addAll(newArchetypes)
    Logger.debug("...done")
    NoContent
  }
  
  /*
  def archetypes(groupId: Option[String], artifactId: Option[String], version: Option[String], description: Option[String]) = DBAction { implicit rs =>
    val archetypes = archetypesService.find(groupId, artifactId, version, description)
    Ok(views.html.index(archetypes))
  }
  */
  
  def archetypeDetails(groupId: String, artifactId: String, version: String, searchGroupId: Option[String], searchArtifactId: Option[String], searchVersion: Option[String], searchDescription: Option[String], filename: Option[String]) = DBAction { implicit rs =>
    val searchData = Some(SearchData(searchGroupId, searchArtifactId, searchVersion, searchDescription))
    val archetypes = archetypesService.find(Some(groupId), Some(artifactId), Some(version), None);
    if (1 <= archetypes.size) {
      val archetype = archetypes.head
      val loadedArchetype = archetypesService.loadArchetypeContent(archetype)
      Logger.debug(s"Loaded archetype: $loadedArchetype")
      Logger.debug(s"basepath: ${archetype.localDir}")
      val fileTree = filename match {
        case None => {
          if (loadedArchetype.localDir.isDefined)
            Some(DirToHtml.toHtml(new File(loadedArchetype.localDir.get)))
          else
            None
        }
        case Some(file) => {
          loadedArchetype.localDir match {
            case None => None
            case Some(dir) => {
              val absoluteUrl = routes.ArchetypesController.archetypeDetails(archetype.groupId, archetype.artifactId, archetype.version, searchGroupId, searchArtifactId, searchVersion, searchDescription, None).absoluteURL(current.configuration.getBoolean("https").get);
              val hrefTemplate = absoluteUrl + ((if (absoluteUrl.contains("?")) "&" else "?") + "file={file}")
              val fileSource = sourcePrettifyService.toPrettyHtml(new File(dir), file)
              Some(DirToHtml.toHtml(new File(dir), file, hrefTemplate))
            }
          }
        }
      }
      val fileSource = 
        if (filename.isDefined && loadedArchetype.localDir.isDefined) {
          Some(sourcePrettifyService.toPrettyHtml(new File(loadedArchetype.localDir.get), filename.get))
        } else {
          None
        }
      //Logger.debug(s"$fileSource")
      Logger.debug(s"filename: $filename")
      Ok(views.html.archetypeDetails(archetype, searchData, fileTree, fileSource))
    } else {
      Logger.error(s"Cannot find $groupId > $artifactId > $version")
      NotFound
    }
  }

  implicit val userReads: Reads[Archetype] = (
    (__ \ "id").readNullable[Long] and
    (__ \ "groupId").read[String] and
    (__ \ "artifactId").read[String] and
    (__ \ "version").read[String] and
    (__ \ "description").readNullable[String] and
    (__ \ "repository").readNullable[String] and
    (__ \ "javaVersion").readNullable[String] and
    Reads.pure(None) and
    Reads.pure(None)
  )(Archetype)
  
  implicit val userWrites = new Writes[Archetype] {
    def writes(a: Archetype): JsValue = {
      Json.obj(
        "groupId" -> a.groupId,
        "artifactId" -> a.artifactId,
        "version" -> a.version,
        "description" -> a.description,
        "repository" -> a.repository,
        "javaVersion" -> a.javaVersion
      )
    }
  }
  
//  implicit val userWrites: Writes[Archetype] = (
//    Writes.pruned and
//    (JsPath \ "groupId").write[Double] and
//    (JsPath \ "long").write[Double]
//  )(unlift(Archetype.unapply))
  /*implicit object ArchetypeFormat extends Format[Archetype] {
    def reads(json: JsValue): JsResult[Archetype] = JsSuccess(Archetype(
      (json \ "id").asOpt[Long] and
      (json \ "groupId").as[String] and
      (json \ "artifactId").as[String] and
      (json \ "version").as[String] and
      (json \ "description").asOpt[String] and
      (json \ "repository").asOpt[String] and
      Reads.pure(None) and
      Reads.pure(None) and
      Reads.pure(None)
    ))
    def writes(a: Archetype): JsValue = JsObject(List(
      "groupId" -> JsString(a.groupId),
      "artifactId" -> JsString(a.artifactId),
      "version" -> JsString(a.version),
      "description" -> JsString(a.description.getOrElse("")),
      "repository" -> JsString(a.repository.getOrElse(""))
    ))
  }
  */

  def restArchetypes(groupId: Option[String], artifactId: Option[String], version: Option[String], description: Option[String]) = DBAction { implicit rs =>
    Ok(Json.toJson(archetypesService.find(groupId, artifactId, version, description)))
  }
  
  def browse(groupId: String, artifactId: String, version: String) = DBAction { implicit rs =>
    val archetypes = archetypesService.find(Some(groupId), Some(artifactId), Some(version), None)
    if (!archetypes.isEmpty) {
      val archetype = archetypes.head
      if (archetype.localDir.isEmpty) {
        NotFound
      } else {
        var dir = Form("dir" -> text).bindFromRequest.get
        if (dir.contains("..")) {
          Logger.error(s"Tried to browse relativa parent dir: $dir");
          NotFound
        } else {
          if (dir.charAt(dir.length()-1) == '\\') {
              dir = dir.substring(0, dir.length()-1) + "/";
          } else if (dir.charAt(dir.length()-1) != '/') {
              dir += "/";
          }
          dir = java.net.URLDecoder.decode(dir, "UTF-8");
          val baseDir = new File(archetype.localDir.get, dir)
          Logger.debug("browsing: " + dir)
          Logger.debug("mapped to: " + baseDir)
          if (baseDir.exists()) {
            val files = baseDir.list
            Arrays.sort(files, String.CASE_INSENSITIVE_ORDER)
            var result = "<ul class=\"jqueryFileTree\" style=\"display: none;\">"
            for (file <- files) {
              if (new File(baseDir, file).isDirectory()) {
                result += "<li class=\"directory collapsed\"><a href=\"#\" rel=\"" + dir + file + "/\">" + file + "</a></li>"
              }
            }
            for (file <- files) {
              if (!new File(baseDir, file).isDirectory()) {
                val dotIndex = file.lastIndexOf('.')
                val ext =  if (dotIndex > 0) file.substring(dotIndex + 1) else ""
                result += "<li class=\"file ext_" + ext + "\"><a href=\"#\" rel=\"" + dir + file + "\">" + file + "</a></li>"
              }
            }
            result += "</ul"
            // Logger.debug(s"Result: $result")
            Ok(result)
          } else {
            NotFound
          }
        }
      }
    } else {
      NotFound
    }
  }
  
  def getFile(groupId: String, artifactId: String, version: String, file: String) = DBAction { implicit rs =>
    if (file.contains("..")) {
      Logger.error(s"Tried to browse relativa parent dir: $file");
      NotFound
    } else {
      val archetypes = archetypesService.find(Some(groupId), Some(artifactId), Some(version), None)
      if (!archetypes.isEmpty) {
        val archetype = archetypes.head
        if (archetype.localDir.isEmpty) {
          NotFound
        } else {
          val downloadFile = new File(archetype.localDir.get, file)
          Logger.debug("Downloading: " + downloadFile.toString())
          Ok(xml.Utility.escape(IOUtils.toString(new FileInputStream(downloadFile))))
        }
      } else {
        NotFound
      }
    }
  }
  
  def loadMetaData(groupId: String, artifactId: String, version: String) = DBAction { implicit rs =>
    Logger.debug(s"Loading for $groupId, $artifactId, $version")
    val archetype = archetypesService.find(Some(groupId), Some(artifactId), Some(version), None).head;
    if (archetype.localDir.isDefined) {
      Logger.debug("Metadata already generated...")
      NoContent
    } else {
      Logger.debug("Generating metadata...")
      val loadedArchetype = archetypesService.loadArchetypeContent(archetype)
      archetypesService.safe(loadedArchetype)
      Logger.debug("done...")
      NoContent
    }
  }
}
