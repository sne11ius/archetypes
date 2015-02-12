import java.io.File
import scala.annotation.implicitNotFound
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import scala.xml.XML
import com.google.inject.Guice
import com.googlecode.htmlcompressor.compressor.HtmlCompressor
import com.mohiva.play.htmlcompressor.HTMLCompressorFilter
import com.mohiva.play.xmlcompressor.XMLCompressorFilter
import play.api.Application
import play.api.Logger
import play.api.Play.current
import play.api.libs.concurrent.Akka
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.WithFilters
import play.filters.gzip.GzipFilter
import services.ArchetypesService
import util.ArchetypesModule
import models.Archetype
import org.apache.maven.artifact.versioning.ComparableVersion
import scala.language.implicitConversions
import org.jsoup.Jsoup
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormatter
import org.joda.time.format.DateTimeFormatterBuilder
import java.util.Locale
import java.io.PrintWriter

object Global extends WithFilters(new GzipFilter(), CustomHTMLCompressorFilter(), XMLCompressorFilter()) {

  val injector = Guice.createInjector(new ArchetypesModule)

  override def getControllerInstance[A](controllerClass: Class[A]): A = injector.getInstance(controllerClass)
  val archetypesService = injector.getInstance(classOf[ArchetypesService])
  
  override def onStart(app: Application) {
    //Akka.system.scheduler.scheduleOnce(1 second) { updateArchetypes }
    //Akka.system.scheduler.scheduleOnce(1 second) { updateLastModified }
    //Akka.system.scheduler.scheduleOnce(1 second) { updateJavaVersions }
  }
    
  implicit def archetypeToComparableVersion(a: Archetype): ComparableVersion = new ComparableVersion(a.version)

  def newest(all: List[Archetype]): List[Archetype] = newestBut(all, 0)
  
  def newestBut(all: List[Archetype], but: Int): List[Archetype] = {
    all.groupBy( a => (a.groupId, a.artifactId)).flatMap {
      case (_, list) => {
        list.sortWith((a1, a2) => { 0 < a1.compareTo(a2) }).drop(but).take(1)
      }
    }.toList.sortBy { a => (a.groupId, a.artifactId, a.version) }
  }
  
  def initial(all: List[Archetype]): List[Archetype] = {
    all.groupBy( a => (a.groupId, a.artifactId)).flatMap {
      case (_, list) => {
        list.sortWith((a1, a2) => { 0 > a1.compareTo(a2) }).take(1)
      }
    }.toList.sortBy { a => (a.groupId, a.artifactId, a.version) }
  }
  
  def updateLastModified = {
    Logger.debug("Updating lastModified...")
    val allArchetypes = archetypesService.findAll
    allArchetypes.zipWithIndex.foreach { e =>
      val archetype = e._1
      val index = e._2
      if (0 != archetype.lastUpdated.getMillis) {
        Logger.debug(s"$index/${ allArchetypes.length } -> Skipping")
      } else {
        Logger.debug(s"$index/${ allArchetypes.length } -> Updating")
        val url = s"https://repo1.maven.org/maven2/${archetype.groupId.replace(".", "/")}/${archetype.artifactId}/${archetype.version}/"
        val doc = Jsoup.connect(url).get();
        val text = doc.select("pre").text
        val matcher = "((([0-9])|([0-2][0-9])|([3][0-1]))\\-(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\\-\\d{4} \\d{2}:\\d{2})"r
        val dateString = matcher.findFirstIn(text).get
        val formatter = new DateTimeFormatterBuilder()
          .appendDayOfMonth(2)
          .appendLiteral("-")
          .appendMonthOfYearShortText()
          .appendLiteral("-")
          .appendYear(4, 4)
          .appendLiteral(" ")
          .appendHourOfDay(2)
          .appendLiteral(":")
          .appendMinuteOfHour(2)
          .toFormatter().withLocale(Locale.ENGLISH)//"dd-mmm-yyyy hh:mm"
        val date = DateTime.parse(dateString, formatter)
        val newArchetype = archetype.copy(lastUpdated = date)
        archetypesService.safe(newArchetype)
      }
    }
  }
  
  def updateArchetypes = {
    var maxCycles = 0
    val allArchetypes = archetypesService.loadFromAllCatalogs.sortBy { a => (a.groupId, a.artifactId, a.version) }
    var but = 0
    
    Logger.debug("Computing max cycles...")
    var testArchetypes = newestBut(allArchetypes, maxCycles)
    while (!testArchetypes.isEmpty) {
      maxCycles += 1
      testArchetypes = newestBut(allArchetypes, maxCycles)
    }
    Logger.debug(s"Max cycles: $maxCycles")
    
    var archetypes = newestBut(allArchetypes, but)
    while (!archetypes.isEmpty) {
      Logger.debug(s"Cycle $but of $maxCycles")
      Logger.debug(s"Importing ${ archetypes.size } `newest - $but' archetypes.")
      Logger.debug("Adding to database...")
      archetypesService.addAllNew(archetypes)
      Logger.debug("...done")
      Logger.debug("Generating details...")
      archetypes.zipWithIndex.foreach {
        case (archetype, index) => {
          val prefix = s"Cycle($but/$maxCycles) -> Archetype(${index + 1}/${archetypes.length})"
          archetypesService.findBy(archetype) match {
            case Some(a) => {
              if (a.localDir.isDefined) {
                //Logger.debug(s"$prefix -> Already generated -> skipping")
              } else {
                //Logger.debug(s"$prefix -> Generating (${archetype.groupId}, ${archetype.artifactId}, ${archetype.version})")
                val loadedArchetype = archetypesService.loadArchetypeContent(archetype)
                if (loadedArchetype.localDir.isEmpty) {
                  Logger.debug(loadedArchetype.toString)
                }
                archetypesService.safe(loadedArchetype)
              }
            }
            case None => {}
          }
        }
      }
      Logger.debug("...done")
      but += 1;
      archetypes = newestBut(archetypesService.loadFromAllCatalogs, but)
    }
  }
  
  def updateJavaVersions = {
    Logger.debug("Updating java versions...")
    val allArchetypes = archetypesService.findAll
    allArchetypes.zipWithIndex.foreach { e =>
      val archetype = e._1
      val index = e._2
      if ("[default]" != archetype.javaVersion.getOrElse("[default]") || archetype.localDir.isEmpty) {
      } else {
        val javaVersion = extractJavaVersion(archetype.localDir.get)
        if (javaVersion != archetype.javaVersion.get) {
          Logger.debug(s"Updated: ${archetype.javaVersion} -> $javaVersion for ${archetype.groupId} / ${archetype.artifactId} / ${archetype.version}")
          archetypesService.safe(archetype.copy(javaVersion = Some(javaVersion)))
        }
      }
    }
    Logger.debug("...done")
  }
  
  private def extractJavaVersion(baseDir: String): String = {
    val file = new File(new File(baseDir, "example-app"), "pom.xml")
    try {
      val xml = XML.loadFile(file)
      var javaVersion = ((xml \ "build" \ "plugins" \ "plugin" \ "configuration" \ "source") text)
      if (javaVersion.contains("$")) {
        val property = javaVersion.drop(2).dropRight(1)
        javaVersion = ((xml \ "properties" \ property) text)
      }
      if ("" == javaVersion) {
        javaVersion = ((xml \ "build" \ "pluginManagement" \ "plugins" \ "plugin" \ "configuration" \ "source") text)
      }
      if (javaVersion.contains("$")) {
        val property = javaVersion.drop(2).dropRight(1)
        javaVersion = ((xml \ "properties" \ property) text)
      }
      if ("" == javaVersion) {
        javaVersion = ((xml \ "properties" \ "maven.compiler.source") text)
      }
      if ("" == javaVersion) {
        javaVersion = ((xml \ "properties" \ "java.version") text)
      }
      if ("" == javaVersion) {
        "[default]"
      } else {
        javaVersion
      }
    } catch {
      case e: Exception => {
        Logger.error(s"Cannot parse $file: ${e getMessage}")
        "[default]"
      }
    }
  }
}

/**
 * Defines a user-defined HTML compressor filter.
 */
object CustomHTMLCompressorFilter {

  /**
   * Creates the HTML compressor filter.
   *
   * @return The HTML compressor filter.
   */
  def apply() = new HTMLCompressorFilter({
    val compressor = new HtmlCompressor()

    compressor.setRemoveComments(true);                                  //if false keeps HTML comments (default is true)
    compressor.setRemoveMultiSpaces(true);                               //if false keeps multiple whitespace characters (default is true)
    compressor.setRemoveIntertagSpaces(true);                            //removes iter-tag whitespace characters
    compressor.setRemoveQuotes(true);                                    //removes unnecessary tag attribute quotes
    compressor.setSimpleDoctype(true);                                   //simplify existing doctype
    compressor.setRemoveScriptAttributes(true);                          //remove optional attributes from script tags
    compressor.setRemoveStyleAttributes(true);                           //remove optional attributes from style tags
    compressor.setRemoveLinkAttributes(true);                            //remove optional attributes from link tags
    compressor.setRemoveFormAttributes(true);                            //remove optional attributes from form tags
    compressor.setRemoveInputAttributes(true);                           //remove optional attributes from input tags
    compressor.setSimpleBooleanAttributes(true);                         //remove values from boolean tag attributes
    compressor.setRemoveJavaScriptProtocol(true);                        //remove "javascript:" from inline event handlers
    compressor.setRemoveHttpProtocol(false);                             //replace "http://" with "//" inside tag attributes
    compressor.setRemoveHttpsProtocol(false);                            //replace "https://" with "//" inside tag attributes
    compressor.setPreserveLineBreaks(false);                             //preserves original line breaks
    compressor.setRemoveSurroundingSpaces("html,div,ul,ol,li,br,p,nav"); //remove spaces around provided tags
    compressor
  })
}
