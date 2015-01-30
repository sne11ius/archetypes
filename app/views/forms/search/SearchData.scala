package views.forms.search

import play.api.data._
import play.api.data.Forms._

object ArchetypeSearch {

  case class SearchData(
    groupId: Option[String],
    artifactId: Option[String],
    version: Option[String],
    description: Option[String],
    javaVersion: Option[String]
  )
  
  val archetypeSearchForm = Form(
    mapping(
      "groupId"     -> optional(text),
      "artifactId"  -> optional(text),
      "version"     -> optional(text),
      "description" -> optional(text),
      "javaVersion" -> optional(text)
    )(SearchData.apply)(SearchData.unapply)
  )
}
