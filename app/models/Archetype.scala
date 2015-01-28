package models

case class Archetype(
  id: Option[Long],
  groupId: String,
  artifactId: String,
  version: String,
  description: Option[String],
  repository: Option[String],
  javaVersion: Option[String],
  localDir: Option[String],
  generateLog: Option[String]
)
