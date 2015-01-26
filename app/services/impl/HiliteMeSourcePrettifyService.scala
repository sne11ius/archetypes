package services.impl

import services.SourcePrettifyService
import javax.inject.Inject
import scala.concurrent.Await
import play.api.libs.ws.WS
import play.api.Play.current
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import java.io.File
import models.Archetype
import org.apache.commons.io.IOUtils
import java.io.FileInputStream
import org.apache.commons.io.FilenameUtils
import play.api.Logger

class HiliteMeSourcePrettifyService @Inject() () extends SourcePrettifyService {

  def toPrettyHtml(baseDir: File, file: String): String = {
    val localFile = new File(baseDir, file)
    val source = IOUtils.toString(new FileInputStream(localFile))
    if ("" == source) {
      "[empty file]"
    } else {
      val lexer = FilenameUtils.getExtension(localFile.toString())
      //Logger.debug(s"source: $source")
      //Logger.debug(s"lexer: $lexer")
      Await.result(WS.url("http://hilite.me/api").withFollowRedirects(true).post(Map(
          "code" -> Seq(source),
          "linenos" -> Seq("yep"),
          "lexer" -> Seq(lexer)
        )).map { response =>
          response.body
        }, 10 seconds)
    }
  }
  
}
