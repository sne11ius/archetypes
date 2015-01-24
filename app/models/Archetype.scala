package models

import play.api.libs.json._
import play.api.libs.functional.syntax._

case class Archetype(
  id: Option[Long],
  groupId: String,
  artifactId: String,
  version: String,
  //javaVersion: String,
  description: Option[String],
  repository: Option[String],
  localDir: Option[String]
)
