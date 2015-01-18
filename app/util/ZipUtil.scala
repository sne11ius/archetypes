package util

import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import play.api.Logger
import java.io.File
import java.io.FileOutputStream
import org.apache.commons.io.IOUtils

object ZipUtil {

  def unzip(src: String, dst: String) = {
    val zipFile = new ZipFile(src)
    val enu = zipFile.entries()
    while (enu.hasMoreElements()) {
      val zipEntry = enu.nextElement()
      val name = zipEntry.getName()
      val size = zipEntry.getSize()
      val compressedSize = zipEntry.getCompressedSize()
      val file = new File(dst, name)
      if (name.endsWith("/")) {
        Logger.debug(s"Creating dir $file")
        file.mkdirs()
      } else {
        Logger.debug(s"Creating file $file")
        val parent = file.getParentFile()
        if (parent != null) {
          parent.mkdirs()
        }
        val is = zipFile.getInputStream(zipEntry)
        val fos = new FileOutputStream(file)
        IOUtils.copy(is, fos)
        is.close();
        fos.close();
      }
    }
    zipFile.close();
  }
  
}
