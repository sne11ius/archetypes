package models

case class Archetype(
  groupId: String,
  artifactId: String,
  version: String,
  description: Option[String],
  repository: Option[String]
)
