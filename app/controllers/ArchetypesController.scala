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
import org.joda.time.DateTime

class ArchetypesController @Inject() (archetypesService: ArchetypesService, sourcePrettifyService: SourcePrettifyService) extends Controller {
  
  def archetypeDetails(groupId: String, artifactId: String, version: String, searchGroupId: Option[String], searchArtifactId: Option[String], searchVersion: Option[String], searchDescription: Option[String], searchJavaVersion: Option[String], filename: Option[String]) = DBAction { implicit rs =>
    val searchData = Some(SearchData(searchGroupId, searchArtifactId, searchVersion, searchDescription, searchJavaVersion))
    val archetypes = archetypesService.find(Some(groupId), Some(artifactId), Some(version), None, None);
    if (1 <= archetypes.size) {
      val loadedArchetype = archetypes.head
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
              val absoluteUrl = routes.ArchetypesController.archetypeDetails(loadedArchetype.groupId, loadedArchetype.artifactId, loadedArchetype.version, searchGroupId, searchArtifactId, searchVersion, searchDescription, searchJavaVersion, None).absoluteURL(current.configuration.getBoolean("https").get)
              val hrefTemplate = absoluteUrl + ((if (absoluteUrl.contains("?")) "&" else "?") + "file={file}")
              val fileSource = sourcePrettifyService.toPrettyHtml(new File(dir, "example-app"), file)
              Some(DirToHtml.toHtml(new File(dir, "example-app"), file, hrefTemplate))
            }
          }
        }
      }
      val file = 
        if (filename.isDefined && loadedArchetype.localDir.isDefined) {
          Some(mkFileDescriptor(loadedArchetype, new File(loadedArchetype.localDir.get, "example-app"), filename.get))
        } else {
          None
        }
      Ok(views.html.archetypeDetails(loadedArchetype, searchData, fileTree, file))
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
    val descriptor = 
      if (0 == file.length()) {
        Empty
      } else {
        MimeUtil.registerMimeDetector("eu.medsea.mimeutil.detector.OpendesktopMimeDetector");
        MimeUtil.registerMimeDetector("eu.medsea.mimeutil.detector.MagicMimeMimeDetector")
        MimeUtil.registerMimeDetector("eu.medsea.mimeutil.detector.ExtensionMimeDetector")
        
        val mimeTypes = MimeUtil.getMimeTypes(file)
        val mimeType = MimeUtil.getMostSpecificMimeType(mimeTypes)
        val extension = FilenameUtils.getExtension(filename.toLowerCase(Locale.ENGLISH))
        val simplename = FilenameUtils.getBaseName(filename.toLowerCase(Locale.ENGLISH))
        Logger.debug(s"MimeType: $mimeType")
        val textTypes = List("xml", "x-javascript", "sql", "jsf", "prefs", "factorypath", "mf", "gitignore", "license", "bnd", "as", "sh", "tfl", "cfg", "editorconfig", "page", "bat", "gitkeep", "hgignore", "sass", "scss", "yml", "json", "mustache", "gradlew", "proto", "pro", "desktop", "rb", "readme", "xhtml")
        val imageTypes = List("svg")
        if ("x-markdown" == mimeType.getSubType) {
          val source = IOUtils.toString(new FileInputStream(file))
          Markdown(new PegDownProcessor(ALL).markdownToHtml(source.trim))
        } else if (imageTypes.contains(extension)) {
          Image(routes.ArchetypesController.getFile(archetype.groupId, archetype.artifactId, archetype.version, filename).absoluteURL(current.configuration.getBoolean("https").get))
        } else if ("text" == mimeType.getMediaType || textTypes.contains(mimeType.getSubType) || textTypes.contains(extension) || textTypes.contains(simplename)) {
          Text(sourcePrettifyService.toPrettyHtml(baseDir, filename))
        } else if ("image" == mimeType.getMediaType) {
          Image(routes.ArchetypesController.getFile(archetype.groupId, archetype.artifactId, archetype.version, filename).absoluteURL(current.configuration.getBoolean("https").get))
        } else {
          //Logger.debug("... binary")
          Binary(routes.ArchetypesController.getFile(archetype.groupId, archetype.artifactId, archetype.version, filename).absoluteURL(current.configuration.getBoolean("https").get))
        }
      }
    Logger.debug(s"Type[$file] -> ${descriptor.getClass.getName}")
    descriptor
  }
  
  def archetypeGenerate(groupId: String, artifactId: String, version: String) = DBAction { implicit rs =>
    var archetypes = archetypesService.find(Some(groupId), Some(artifactId), Some(version), None, None)
    if (1 <= archetypes.size) {
      val archetype = archetypes.head
      val props = scala.collection.mutable.Map(
        "groupId" -> "com.example",
        "artifactId" -> "artifact",
        "version" -> "1.0.0-SNAPSHOT",
        "projectName" -> "My Project Name"
      )
      archetype.additionalProps.map { s =>
        props += (s -> s"My$s") 
      }
      rs.body.asFormUrlEncoded match {
        case None => {
          Logger.debug("No form data D:")
          Ok(views.html.archetypeGenerate(archetype, props.toMap))
        }
        case Some(map) => {
          val yourGroupId = map.get("groupId").get.head
          val yourArtifactId = map.get("artifactId").get.head
          val yourVersion = map.get("version").get.head
          val bytes = archetypesService.generate(archetype, map.map(e => (e._1, e._2.head)))
          Ok(bytes).withHeaders(CONTENT_DISPOSITION -> s"attachment; filename=${yourGroupId}.${yourArtifactId}.${yourVersion}.zip")
        }
      }
    } else {
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
    (__ \ "packaging").readNullable[String] and
    (__ \ "lastUpdated").read[DateTime] and
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
        "lastUpdated" -> a.lastUpdated,
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
