package models

case class MavenGenerateResult (
  exitValue: Int,
  stdout: String,
  additionalProps: List[String]
)
