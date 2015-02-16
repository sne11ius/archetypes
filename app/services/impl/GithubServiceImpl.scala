package services.impl

import services.GithubService
import javax.inject.Inject
import org.kohsuke.github.GitHub
import play.api.Logger
import play.api.Play.current
import scala.language.postfixOps
import models.Archetype
import java.util.Locale
import java.util.UUID
import java.io.File
import org.zeroturnaround.zip.ZipUtil
import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.util.function.Predicate
import java.nio.file.Path
import java.util.function.Consumer
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import java.io.FileInputStream

class GithubServiceImpl @Inject() () extends GithubService {
  
  private def rootDir: String = {
    val osString = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH);
    val WinMatcher = "win"r
    val result = osString match {
      case WinMatcher(_) => current.configuration.getString("tempDirWindows").get
      case _ => current.configuration.getString("tempDir").get
    }
    result
  }
  
  override def createRepo(reponame: String, zipData: Array[Byte], archetype: Archetype, accessToken: String): Option[String] = {
    val github = GitHub.connectUsingOAuth(accessToken)
    val existingRepo = github.getMyself.getRepositories.get(reponame)
    if (null != existingRepo) {
      Logger.debug(s"Repo already exists D:")
      None
    } else {
      val repo = github.createRepository(
        reponame,
        "",
        "",
        true
      );
      Logger.debug(s"Extracting to base dir: $rootDir")
      val baseDir = new File(rootDir, UUID.randomUUID().toString)
      if (!baseDir.mkdirs()) {
        Logger.debug(s"Cannot create baseDir: $baseDir")
        None
      } else {
        ZipUtil.unpack(new ByteArrayInputStream(zipData), baseDir)
        try {
          Files.walk(baseDir.toPath()).filter(new Predicate[Path]() {
            override def test(p: Path): Boolean = {
              p.toFile.isFile
            }
          }).forEach(new Consumer[Path]() {
            override def accept(p: Path) = {
              val fileName = new File(baseDir, reponame).toPath().relativize(p)
              val fileData = IOUtils.toByteArray(new FileInputStream(p.toFile))
              repo.createContent(fileData, s"[wasis.nu/mit/archetyes] Adding initial file $fileName", fileName.toString)
              val sleep = 50L
              Thread.sleep(sleep)
            }
          })
          FileUtils.deleteDirectory(baseDir)
          Some(repo.getSvnUrl)
        } catch {
          case e: Exception => {
            Logger.error("err'd D:", e)
            None
          }
        }
      }
    }
  }
  
}
