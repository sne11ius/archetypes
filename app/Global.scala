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
import org.joda.time.Interval
import org.joda.time.format.DateTimeFormatter
import org.joda.time.format.DateTimeFormatterBuilder
import java.util.Locale
import java.io.PrintWriter
import scala.concurrent.duration._

object Global extends WithFilters(new GzipFilter(), CustomHTMLCompressorFilter(), XMLCompressorFilter()) {

  val injector = Guice.createInjector(new ArchetypesModule)

  override def getControllerInstance[A](controllerClass: Class[A]): A = injector.getInstance(controllerClass)
  val archetypesService = injector.getInstance(classOf[ArchetypesService])
  
  override def onStart(app: Application) {
    //Akka.system.scheduler.scheduleOnce(1 second) { updateArchetypes }
    //Akka.system.scheduler.scheduleOnce(1 second) { updateLastModified }
    //Akka.system.scheduler.scheduleOnce(1 second) { updateJavaVersions }
    //Akka.system.scheduler.scheduleOnce(1 second) { showNewArchetypes }
    //Akka.system.scheduler.scheduleOnce(1 second) { showInitialArchetypes }
    Akka.system.scheduler.scheduleOnce(1 second) { addNewArchetypes }
    Akka.system.scheduler.schedule(timeTillMidnight, 1.days) { addNewArchetypes }
  }
  
  private def timeTillMidnight : FiniteDuration = {
    val now      = new DateTime
    val midnight = now.toLocalDate.plusDays(1).toDateTimeAtStartOfDay(now.getZone)
    val millisToMidnight = new Interval(now, midnight).toDurationMillis
    new FiniteDuration(millisToMidnight, MILLISECONDS)
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
  
  def addNewArchetypes = {
    Logger.debug("New releases")
    Logger.debug("Loading from maven central...")
    val allArchetypes = archetypesService.loadFromAllCatalogs.sortBy { a => (a.groupId, a.artifactId, a.version) }
    Logger.debug("...done")
    Logger.debug("Filtering...")
    val unknown = allArchetypes.filter { a =>
      archetypesService.findBy(a).isEmpty
    }
    Logger.debug("...done")
    Logger.debug(s"New archetypes:\n$unknown")
    Logger.debug(s"# new archetypes: ${unknown.size}")
    if (0 < unknown.size) {
      Logger.debug("Adding new Archetypes...")
      val total = unknown.size
      unknown.zipWithIndex.map { case (a, index) =>
        Logger.debug(s"(${index + 1} / $total)")
        archetypesService.safe(a)
        val loadedArchetype = archetypesService.loadArchetypeContent(a)
        if (loadedArchetype.localDir.isEmpty) {
          Logger.debug(s"Cannot load $loadedArchetype")
        } else {
          archetypesService.safe(loadedArchetype)
        }
      }
    } else {
      Logger.debug("Nothing new...")
    }
    Logger.debug("...done")
  }
  
  def showNewArchetypes = {
    Logger.debug("New releases")
    Logger.debug("Loading from maven central...")
    val allArchetypes = archetypesService.loadFromAllCatalogs.sortBy { a => (a.groupId, a.artifactId, a.version) }
    Logger.debug("...done")
    Logger.debug("Filtering...")
    val unknown = allArchetypes.filter { a =>
      archetypesService.findBy(a).isEmpty
    }
    Logger.debug("...done")
    Logger.debug(s"New archetypes:\n$unknown")
    Logger.debug(s"# new archetypes: ${unknown.size}")
  }
  
  def showInitialArchetypes = {
    Logger.debug("Initial releases")
    Logger.debug("Loading from maven central...")
    val newArchetypes = archetypesService.loadFromAllCatalogs.sortBy { a => (a.groupId, a.artifactId, a.version) }
    Logger.debug("...done")
    val knownArchetypes = archetypesService.findAll
    Logger.debug("Filtering...")
    val unknown = newArchetypes.filter { a =>
      knownArchetypes.filter { x =>
        x.groupId == a.groupId && x.artifactId == a.artifactId
      }.isEmpty
    }
    Logger.debug("...done")
    Logger.debug(s"New archetypes:\n$unknown")
    Logger.debug(s"# new archetypes: ${unknown.size}")
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
