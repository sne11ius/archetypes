import play.api._
import play.api.Play.current
import play.api.mvc._

import com.google.inject.{Guice, AbstractModule}
import play.api.GlobalSettings
import services.impl.ArchetypesServiceImpl
import services.ArchetypesService
import util.ArchetypesModule
import com.mohiva.play.htmlcompressor.HTMLCompressorFilter
import com.googlecode.htmlcompressor.compressor.HtmlCompressor
import com.mohiva.play.xmlcompressor.XMLCompressorFilter
import play.filters.gzip.GzipFilter

object Global extends WithFilters(new GzipFilter(), HTMLCompressorFilter(), XMLCompressorFilter()) {

  val injector = Guice.createInjector(new ArchetypesModule)

  override def getControllerInstance[A](controllerClass: Class[A]): A = injector.getInstance(controllerClass)
  
}

/**
 * Defines a user-defined HTML compressor filter.
 */
object HTMLCompressorFilter {

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