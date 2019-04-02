package shuttlecraft

import java.io.File
import java.nio.file.{Path, Paths}

import shuttlecraft.publishing.{FileResource, Resource}
import guillotine._

import scala.util.Try

class Gpg(passphrase: Option[String]) {

  def signature(resource: Resource)(implicit workingDir: Path): Try[Resource] = {
    for{
      storage <- Try(File.createTempFile(resource.locator.replace('/', '_'), ".tmp", workingDir.toFile))
      _ = storage.deleteOnExit()
      _ <- resource.save(storage.toPath)
      signatureStorage <- poorMansSign(storage.toPath, passphrase)
    } yield FileResource(resource.locator + ".asc", signatureStorage)
  }

  // http://central.sonatype.org/pages/working-with-pgp-signatures.html#signing-a-file
  private def poorMansSign(file: Path, passphrase: Option[String]): Try[Path] = {
    import guillotine.environments.none
    val fileName = file.toString
    val cmd = passphrase match {
      case Some(pass) =>
        sh"gpg --passphrase $pass --batch --yes -a -b $fileName"
      case None =>
        sh"gpg --batch --yes -a -b $fileName"
    }
    cmd.exec[Try[String]].map{ _ =>
      Paths.get(fileName + ".asc")
    }

  }

}
