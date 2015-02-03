package services.impl

import java.io.File
import java.util.Locale
import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConversions.seqAsJavaList
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.language.implicitConversions
import scala.language.postfixOps
import scala.sys.process.BasicIO
import scala.sys.process.Process
import scala.sys.process.ProcessBuilder
import scala.util.matching.Regex
import scala.xml.XML
import org.apache.maven.artifact.versioning.ComparableVersion
import extensions.MyScalaExtensions.ExtendedNodeSeq
import javax.inject.Inject
import models.Archetype
import models.MavenGenerateResult
import models.daos.ArchetypeDao
import play.api.Logger
import play.api.Play.current
import play.api.libs.ws.WS
import services.ArchetypesService
import org.joda.time.DateTime
import org.jsoup.Jsoup
import org.joda.time.format.DateTimeFormatterBuilder
import org.apache.commons.io.IOUtils
import org.apache.commons.io.FileUtils
  
class ArchetypesServiceImpl @Inject() (archetypesDao: ArchetypeDao) extends ArchetypesService {
  
  implicit val context = play.api.libs.concurrent.Execution.Implicits.defaultContext
  
  private def rootDir: String = {
    val osString = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH);
    val WinMatcher = "win"r
    val result = osString match {
      case WinMatcher(_) => current.configuration.getString("tempDirWindows").get
      case _ => current.configuration.getString("tempDir").get
    }
    //Logger.debug(s"Base directory: $result")
    result
  }
  
  override def loadFromAllCatalogs: List[Archetype] = {
    current.configuration.getStringList("archetypes.catalogs").map(_.toList).get.flatMap { url =>
      Await.result(WS.url(url).withFollowRedirects(true).get().map { response =>
        response.xml \\ "archetype-catalog" \\ "archetypes" \\ "archetype" map { a =>
          Archetype(
            None,
            (a \ "groupId").text,
            (a \ "artifactId").text,
            (a \ "version").text,
            (a \ "description").textOption,
            (a \ "repository").textOption,
            None,
            None,
            new DateTime(0),
            None,
            None,
            List()
          )
        }
      }, 10 seconds)
    }.distinct
  }
  
  override def findAll: List[Archetype] = {
    archetypesDao.findAll
  }

  override def addAllNew(archetypes: List[Archetype]) = {
    Logger.warn("addAllNew will only add _new_ archetypes")
    archetypes.map { a =>
      findBy(a) match {
        case None => {
          archetypesDao.safe(a)
        }
        case _ => {}
      }
    }
  }
  
  override def safe(archetype: Archetype): Unit = {
    archetypesDao.safe(archetype)
  }
  
  override def findBy(ex: Archetype): Option[Archetype] = {
    archetypesDao.findBy(ex)
  }
  
  override def find(groupId: Option[String], artifactId: Option[String], version: Option[String], description: Option[String], javaVersion: Option[String]): List[Archetype] = {
    //This will be slow as hell, but I cannot slick, so...
    val allArchetypes = archetypesDao.findAll
    //Logger.debug(s"Total # archetypes: ${allArchetypes.size}")
    val archetypes = archetypesDao.findAll.filter { a =>
      if (groupId.isDefined) {
        a.groupId.toLowerCase().contains(groupId.get.toLowerCase)
      } else {
        true
      }
    }.filter { a =>
      if (artifactId.isDefined) {
        a.artifactId.toLowerCase().contains(artifactId.get.toLowerCase)
      } else {
        true
      }
    }.filter { a =>
      if (description.isDefined) {
        description.get.split("[\\p{Punct}\\s]+").filterNot { s => s.trim().isEmpty() }.exists { s =>
          a.description.getOrElse("").toLowerCase().contains(s.toLowerCase)
        }
      } else {
        true
      }
    }.filter { a =>
      if (version.isDefined && version.get != "newest") {
        a.version.toLowerCase().contains(version.get.toLowerCase)
      } else {
        true
      }
    }.filter { a =>
      if (javaVersion.isDefined) {
        a.javaVersion == javaVersion
      } else {
        true
      }
    }
    val result = 
      if (version.isDefined && version.get == "newest") {
        archetypes.groupBy( a => (a.groupId, a.artifactId)).flatMap {
          case ((groupId, artifactId), list) => {
            list.sortWith((a1, a2) => { 0 < a1.compareTo(a2) }).take(1)
          }
        }.toList
      } else {
        archetypes
      }
    //Logger.debug(s"Total # archetypes after filter: ${result.size}")
    result.sortBy(a => (a.groupId, a.artifactId))
  }
  
  implicit def archetypeToComparableVersion(a: Archetype) : ComparableVersion = new ComparableVersion(a.version)
  
  private def buildFilename(prefix: String, archetype: Archetype): String = {
    List(
      prefix,
      archetype.groupId.replace(".", File.separator),
      archetype.artifactId,
      archetype.version
    ).mkString(File.separator)
  }
  
  private def archetypeGenerate(archetype: Archetype, groupId: String, artifactId: String, baseDir: String): MavenGenerateResult = {
    // Logger.debug(s"Creating $baseDir")
    val baseFile = new File(baseDir)
    if (baseFile.exists()) {
      Logger.debug(s"Deleting already existing base directory: $baseFile...")
      FileUtils.deleteDirectory(baseFile)
      Logger.debug("...done")
    }
    if (!(new File(baseDir).mkdirs())) {
      Logger.error(s"Cannot mkdir: $baseDir")
      MavenGenerateResult(-1, s"Cannot create base directory: $baseDir", List[String]())
    } else {
      val sb = new StringBuffer
      val process = makeProcess(archetype, groupId, artifactId, baseDir, List())
      var exitValue = process.run(BasicIO(false, sb, None)).exitValue
      var additionalProps = List[String]();
      if (0 != exitValue) {
        val log = sb.toString
        val props = extractAdditionalProps(log)
        if (!props.isEmpty) {
          sb.setLength(0)
          Logger.debug(s"Retrying with additional props: $props")
          val process = makeProcess(archetype, groupId, artifactId, baseDir, props)
          exitValue = process.run(BasicIO(false, sb, None)).exitValue
          if (0 == exitValue) {
            //Logger.debug("Fixed it.")
            additionalProps = props
          } else {
            Logger.debug("No halp D:")
            Logger.debug(sb.toString)
          }
        }
      }
      MavenGenerateResult(exitValue, sb.toString.replace(baseDir, "").replace("\\", "/"), additionalProps)
    }
  }
  
  private def makeProcess(archetype: Archetype, groupId: String, artifactId: String, baseDir: String, additionalProps: List[String]): ProcessBuilder = {
    val propsList = additionalProps.map( p => {
      "-D" + p + "=Example" + p.split("-").map(s => s.take(1).toUpperCase(Locale.ENGLISH) + s.drop(1)).mkString
    })
    val dafaultCmds = List[String](
     "mvn",
      "archetype:generate",
      "-DinteractiveMode=false",
      s"-DarchetypeGroupId=${archetype.groupId}",
      s"-DarchetypeArtifactId=${archetype.artifactId}",
      s"-DarchetypeVersion=${archetype.version}",
      s"-DgroupId=$groupId",
      s"-DartifactId=$artifactId",
      "-DprojectName=ExampleProject"
    )
    Process((new java.lang.ProcessBuilder(dafaultCmds ++ propsList)) directory new File(baseDir))
  }
  
  private def extractAdditionalProps(errorLog: String): List[String] = {
    val matcher = "(\\[ERROR\\] Property )(.*)( is missing.)"r: Regex
    matcher.findAllIn(errorLog).matchData.map ( m => m.group(2) ).toList.filter(s => !s.contains(",") && !s.contains(" "))
  }
  
  override def loadArchetypeContent(externArchetype: Archetype): Archetype = {
    findBy(externArchetype) match {
      case Some(archetype) => {
        val baseDir = buildFilename(rootDir, archetype)
        if (archetype.localDir.isDefined) {
          Logger.debug(s"localDir already defined as: ${archetype.localDir}")
          archetype
        } else {
          // Logger.debug(s"baseDir: $baseDir")
          archetypeGenerate(archetype, "com.example", "example-app", baseDir) match {
            case MavenGenerateResult(0, stdout, additionalProps) => {
              //Logger.debug(stdout)
              //Logger.debug(additionalProps.toString)
              archetype.copy(
                javaVersion = Some(extractJavaVersion(baseDir)),
                packaging = Some(extractPackaging(baseDir)),
                lastUpdated = getLastUpdated(archetype),
                localDir = Some(baseDir),
                generateLog = Some(stdout),
                additionalProps = additionalProps
              )
            }
            case MavenGenerateResult(_, stdout, _) => {
              //Logger.error(stdout)
              archetype.copy(
                generateLog = Some(stdout),
                lastUpdated = getLastUpdated(archetype)
              )
            }
          }
        }
      }
      case None => {
        Logger.error(s"Not found: $externArchetype")
        throw new RuntimeException(s"Cannot load content for unknown archetype: $externArchetype")
      }
    }
  }
  
  private def extractJavaVersion(baseDir: String): String = {
    val file = new File(new File(baseDir, "example-app"), "pom.xml")
    try {
      val xml = XML.loadFile(file)
      var javaVersion = ((xml \ "build" \ "plugins" \ "plugin" \ "configuration" \ "source") text)
      if (javaVersion.contains("$")) {
        //Logger.debug(s"We need to go deeper for $file")
        val property = javaVersion.drop(2).dropRight(1)
        javaVersion = ((xml \ "properties" \ property) text)
      }
      //Logger.debug(s"Java version: $javaVersion")
      if ("" == javaVersion) {
        javaVersion = ((xml \ "properties" \ "maven.compiler.source") text)
      }
      if ("" == javaVersion) {
        "[default]"
      } else {
        javaVersion
      }
    } catch {
      case e: Exception => {
        Logger.debug(s"Cannot parse $file: ${e getMessage}")
        "[default]"
      }
    }
  }
  
  private def extractPackaging(baseDir: String): String = {
    val file = new File(new File(baseDir, "example-app"), "pom.xml")
    try {
      val xml = XML.loadFile(file)
      var packaging = ((xml \ "packaging") text)
      if ("" == packaging) {
        packaging = "jar"
      }
      packaging
    } catch {
      case e: Exception => {
        Logger.debug(s"Cannot parse $file: ${e getMessage}")
        "jar"
      }
    }
  }

  private def getLastUpdated(archetype: Archetype): DateTime = {
    val url = s"https://repo1.maven.org/maven2/${archetype.groupId.replace(".", "/")}/${archetype.artifactId}/${archetype.version}/"
    val doc = Jsoup.connect(url).get();
    val text = doc.select("pre").text
    val matcher = "((([0-9])|([0-2][0-9])|([3][0-1]))\\-(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\\-\\d{4} \\d{2}:\\d{2})"r
    val dateString = matcher.findFirstIn(text).get
    dateTimeFormatter.parseDateTime(dateString)
  }

  private val dateTimeFormatter = new DateTimeFormatterBuilder()
      .appendDayOfMonth(2)
      .appendLiteral("-")
      .appendMonthOfYearShortText()
      .appendLiteral("-")
      .appendYear(4, 4)
      .appendLiteral(" ")
      .appendHourOfDay(2)
      .appendLiteral(":")
      .appendMinuteOfHour(2)
      .toFormatter()
      .withLocale(Locale.ENGLISH)//"dd-mmm-yyyy hh:mm"

}
