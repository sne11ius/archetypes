package controllers

import java.io.File
import java.util.Arrays
import javax.inject.Inject
import models.Archetype
import models.FileDescriptor
import models.Text
import models.Image
import models.Binary
import models.Empty
import models.Markdown
import play.api._
import play.api.Play.current
import play.api.data._
import play.api.data.Forms._
import play.api.db.slick.DBAction
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.libs.json.Json
import play.api.mvc._
import services.ArchetypesService
import services.SourcePrettifyService
import views.forms.search.ArchetypeSearch._
import nu.wasis.dir2html.DirToHtml
import eu.medsea.mimeutil.MimeUtil
import org.apache.commons.io.FilenameUtils
import java.util.Locale
import org.pegdown.PegDownProcessor
import org.pegdown.Extensions.ALL
import org.apache.commons.io.IOUtils
import java.io.FileInputStream

class ArchetypesController @Inject() (archetypesService: ArchetypesService, sourcePrettifyService: SourcePrettifyService) extends Controller {
  
  def archetypeDetails(groupId: String, artifactId: String, version: String, searchGroupId: Option[String], searchArtifactId: Option[String], searchVersion: Option[String], searchDescription: Option[String], searchJavaVersion: Option[String], filename: Option[String]) = DBAction { implicit rs =>
    val searchData = Some(SearchData(searchGroupId, searchArtifactId, searchVersion, searchDescription, searchJavaVersion))
    val archetypes = archetypesService.find(Some(groupId), Some(artifactId), Some(version), None, None);
    if (1 <= archetypes.size) {
      val archetype = archetypes.head
      val loadedArchetype = archetypesService.loadArchetypeContent(archetype)
      //Logger.debug(s"Loaded archetype: $loadedArchetype")
      //Logger.debug(s"basepath: ${archetype.localDir}")
      val fileTree = filename match {
        case None => {
          if (loadedArchetype.localDir.isDefined)
            Some(DirToHtml.toHtml(new File(loadedArchetype.localDir.get, "example-app")))
          else
            None
        }
        case Some(file) => {
          loadedArchetype.localDir match {
            case None => None
            case Some(dir) => {
              val absoluteUrl = routes.ArchetypesController.archetypeDetails(archetype.groupId, archetype.artifactId, archetype.version, searchGroupId, searchArtifactId, searchVersion, searchDescription, searchJavaVersion, None).absoluteURL(current.configuration.getBoolean("https").get)
              val hrefTemplate = absoluteUrl + ((if (absoluteUrl.contains("?")) "&" else "?") + "file={file}")
              val fileSource = sourcePrettifyService.toPrettyHtml(new File(dir, "example-app"), file)
              Some(DirToHtml.toHtml(new File(dir, "example-app"), file, hrefTemplate))
            }
          }
        }
      }
      val file = 
        if (filename.isDefined && loadedArchetype.localDir.isDefined) {
          Some(mkFileDescriptor(archetype, new File(loadedArchetype.localDir.get, "example-app"), filename.get))
        } else {
          None
        }
      //Logger.debug(s"$fileSource")
      //Logger.debug(s"filename: $filename")
      Ok(views.html.archetypeDetails(archetype, searchData, fileTree, file))
    } else {
      Logger.error(s"Cannot find $groupId > $artifactId > $version")
      NotFound
    }
  }
  
  private def mkFileDescriptor(archetype: Archetype, baseDir: File, filename: String)(implicit request: RequestHeader): FileDescriptor = {
    val file = new File(baseDir, filename)
    if (!file.exists) {
      Logger.error(s"File does not exist: $file")
    }
    if (0 == file.length()) {
      Empty
    } else {
      Logger.debug(s"Detecting $file...")
      MimeUtil.registerMimeDetector("eu.medsea.mimeutil.detector.OpendesktopMimeDetector");
      MimeUtil.registerMimeDetector("eu.medsea.mimeutil.detector.MagicMimeMimeDetector")
      MimeUtil.registerMimeDetector("eu.medsea.mimeutil.detector.ExtensionMimeDetector")
      
      val mimeTypes = MimeUtil.getMimeTypes(file)
      val mimeType = MimeUtil.getMostSpecificMimeType(mimeTypes)
      val extension = FilenameUtils.getExtension(filename.toLowerCase(Locale.ENGLISH))
      val simplename = FilenameUtils.getBaseName(filename.toLowerCase(Locale.ENGLISH))
      Logger.debug(s"MimeType: $mimeType")
      //Logger.debug(s"Extension: $extension")
      // MimeUtil.isTextMimeType does not work :/
      val textTypes = List("xml", "x-javascript", "sql", "jsf", "prefs", "factorypath", "mf", "gitignore", "license", "bnd", "as")
      if ("x-markdown" == mimeType.getSubType) {
        val source = IOUtils.toString(new FileInputStream(file))
        Logger.debug("... markdown")
        Markdown(new PegDownProcessor(ALL).markdownToHtml(source.trim))
      } else if ("text" == mimeType.getMediaType || textTypes.contains(mimeType.getSubType) || textTypes.contains(extension) || textTypes.contains(simplename)) {
        Logger.debug("... text")
        Text(sourcePrettifyService.toPrettyHtml(baseDir, filename))
      } else if ("image" == mimeType.getMediaType) {
        Logger.debug("... image")
        Image(routes.ArchetypesController.getFile(archetype.groupId, archetype.artifactId, archetype.version, filename).absoluteURL(current.configuration.getBoolean("https").get))
      } else {
        Logger.debug("... binary")
        Binary(routes.ArchetypesController.getFile(archetype.groupId, archetype.artifactId, archetype.version, filename).absoluteURL(current.configuration.getBoolean("https").get))
      }
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
    (__ \ "packaging").readNullable[String] and
    Reads.pure(None) and
    Reads.pure(None) and
    (__ \ "additionalProps").read[List[String]]
  )(Archetype)
  
  implicit val userWrites = new Writes[Archetype] {
    def writes(a: Archetype): JsValue = {
      Json.obj(
        "groupId" -> a.groupId,
        "artifactId" -> a.artifactId,
        "version" -> a.version,
        "description" -> a.description,
        "repository" -> a.repository,
        "javaVersion" -> a.javaVersion,
        "packaging" -> a.packaging,
        "additionalProps" -> a.additionalProps
      )
    }
  }

  def restArchetypes(groupId: Option[String], artifactId: Option[String], version: Option[String], description: Option[String]) = DBAction { implicit rs =>
    Ok(Json.toJson(archetypesService.find(groupId, artifactId, version, description, None)))
  }
  
  def getFile(groupId: String, artifactId: String, version: String, file: String) = DBAction { implicit rs =>
    if (file.contains("..")) {
      Logger.error(s"Tried to browse relative parent dir: $file");
      NotFound
    } else {
      val archetypes = archetypesService.find(Some(groupId), Some(artifactId), Some(version), None, None)
      if (!archetypes.isEmpty) {
        val archetype = archetypes.head
        if (archetype.localDir.isEmpty) {
          NotFound
        } else {
          val downloadFile = new File(new File(archetype.localDir.get, "example-app"), file)
          Logger.debug("Downloading: " + downloadFile.toString())
          Ok.sendFile(downloadFile)//, inline, fileName, onClose)(new FileInputStream(downloadFile))
          //Ok(xml.Utility.escape(IOUtils.toString(new FileInputStream(downloadFile))))
        }
      } else {
        NotFound
      }
    }
  }
  
  def loadMetaData(groupId: String, artifactId: String, version: String) = DBAction { implicit rs =>
    Logger.debug(s"Loading for $groupId, $artifactId, $version")
    val archetype = archetypesService.find(Some(groupId), Some(artifactId), Some(version), None, None).head;
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
