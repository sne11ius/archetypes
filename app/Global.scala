import com.google.inject.{Guice, AbstractModule}
import play.api.GlobalSettings
import services.impl.ArchetypesServiceImpl
import services.ArchetypesService
import util.ArchetypesModule

object Global extends GlobalSettings {

   val injector = Guice.createInjector(new ArchetypesModule)

  override def getControllerInstance[A](controllerClass: Class[A]): A = injector.getInstance(controllerClass)
}
