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
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.StringEscapeUtils
import org.pegdown.PegDownProcessor
import org.pegdown.Extensions.ALL
import play.api.Logger

class HiliteMeSourcePrettifyService @Inject() () extends SourcePrettifyService {

  def toPrettyHtml(baseDir: File, file: String): String = {
    val localFile = new File(baseDir, file)
    val source = IOUtils.toString(new FileInputStream(localFile))
    val lexer = FilenameUtils.getExtension(localFile.toString())
              .replace("fxml", "xml")
              .replace("ftl", "html")
              .replace("mustache", "html")
              .replace("classpath", "xml")
              .replace("tmx", "xml")
              .replace("launch", "xml")
              .replace("xsd", "xml")
              .replace("xsl", "xml")
              .replace("zul", "xml")
              .replace("xhtml", "html")
    Await.result(WS.url("http://hilite.me/api").withFollowRedirects(true).post(Map(
        "code" -> Seq(source),
        "linenos" -> Seq("yes, i want line numbers"),
        "lexer" -> Seq(lexer)
      )).map { response =>
        if (200 == response.status) {
          response.body
        } else {
          StringEscapeUtils.escapeHtml4(source).replace("\n", "<br>")
        }
      }, 10 seconds)
  }
  
}
