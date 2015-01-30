package services.impl

import java.io.File
import java.io.FileInputStream

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.IOUtils

import javax.inject.Inject
import play.api.Play.current
import play.api.libs.ws.WS
import services.SourcePrettifyService

class HiliteMeSourcePrettifyService @Inject() () extends SourcePrettifyService {

  def toPrettyHtml(baseDir: File, file: String): String = {
    val localFile = new File(baseDir, file)
    val source = IOUtils.toString(new FileInputStream(localFile))
    if ("" == source) {
      "[empty file]"
    } else {
      val lexer = FilenameUtils.getExtension(localFile.toString())
      Await.result(WS.url("http://hilite.me/api").withFollowRedirects(true).post(Map(
          "code" -> Seq(source),
          "linenos" -> Seq("yes, i want line numbers"),
          "lexer" -> Seq(lexer)
        )).map { response =>
          if (200 == response.status) {
            response.body
          } else {
            source.replace("\n", "<br>")
          }
        }, 10 seconds)
    }
  }
  
}
