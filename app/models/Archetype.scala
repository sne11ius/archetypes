package models

import org.joda.time.DateTime

case class Archetype(
  id: Option[Long],
  groupId: String,
  artifactId: String,
  version: String,
  description: Option[String],
  repository: Option[String],
  javaVersion: Option[String],
  packaging: Option[String],
  lastUpdated: DateTime,
  localDir: Option[String],
  generateLog: Option[String],
  additionalProps: List[String]
)
