package util

import com.google.inject.{/* Provides, */AbstractModule }
import net.codingwell.scalaguice.ScalaModule
import services.impl._
import services._
import models.daos.ArchetypeDao
import models.daos.slick.ArchetypeDaoSlick

class ArchetypesModule extends AbstractModule with ScalaModule {
    def configure() {
      //bind(classOf[ArchetypesService]).to(classOf[ArchetypesServiceImpl])
      //bind(classOf[ArchetypeDao]).to(classOf[ArchetypeDaoSlick])
      bind[ArchetypesService].to[ArchetypesServiceImpl]
      bind[ArchetypeDao].to[ArchetypeDaoSlick]
      bind[SourcePrettifyService].to[HiliteMeSourcePrettifyService]
      bind[controllers.Application]
    }
}
