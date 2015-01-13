package util

import com.google.inject.{/* Provides, */AbstractModule }
import net.codingwell.scalaguice.ScalaModule
import services.impl.ArchetypesServiceImpl
import services.ArchetypesService
import models.ArchetypeDao
import models.daos.ArchetypeDaoSlick

class ArchetypesModule extends AbstractModule with ScalaModule {
    def configure() {
      bind(classOf[ArchetypesService]).to(classOf[ArchetypesServiceImpl])
      bind(classOf[ArchetypeDao]).to(classOf[ArchetypeDaoSlick])
      bind[controllers.Application]
    }
}