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

object Global extends WithFilters(new GzipFilter(), CustomHTMLCompressorFilter(), XMLCompressorFilter()) {

  val injector = Guice.createInjector(new ArchetypesModule)

  override def getControllerInstance[A](controllerClass: Class[A]): A = injector.getInstance(controllerClass)
  val archetypesService = injector.getInstance(classOf[ArchetypesService])
  
  override def onStart(app: Application) {
    Akka.system.scheduler.scheduleOnce(1 second) { updateArchetypes }
  }
    
  implicit def archetypeToComparableVersion(a: Archetype): ComparableVersion = new ComparableVersion(a.version)

  def newest(all: List[Archetype]): List[Archetype] = {
    all.groupBy( a => (a.groupId, a.artifactId)).flatMap {
      case ((groupId, artifactId), list) => {
        list.sortWith((a1, a2) => { 0 < a1.compareTo(a2) }).take(1)
      }
    }.toList
  }
  
  def updateArchetypes = {
    Logger.debug("Updating database...")
    Logger.debug("Loading archetypes...")
    val newArchetypes = newest(archetypesService.loadFromAllCatalogs)
    Logger.debug(s"${newArchetypes.size} archetypes loaded.")
    Logger.debug("Adding to database...")
    archetypesService.addAllNew(newArchetypes)
    Logger.debug("...done")
    val newestArchetypes = archetypesService.find(None, None, Some("newest"), None, None)
    Logger.debug(s"${newestArchetypes.length} 'newest' archetypes")
    Logger.debug("Generating projects...")
    newestArchetypes.zipWithIndex.foreach {
      case (archetype, index) => {
        Logger.debug(s"${index + 1}/${newestArchetypes.length} -> Generating $archetype")
        val loadedArchetype = archetypesService.loadArchetypeContent(archetype)
        // Logger.debug(s"Maven log:")
        // Logger.debug(s"${loadedArchetype.generateLog}");
        archetypesService.safe(loadedArchetype)
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
