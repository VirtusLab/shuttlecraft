package shuttlecraft.repository

import java.io.File
import java.net.{URI, URL}
import java.nio.file.{Path, Paths}

import shuttlecraft._

import scala.util.Try
import NexusRepository._


class NexusRepository(val uri: String, val snapshotUri: String) extends Repository[(Username, Password)] {

  override val url: URL = new URI(snapshotUri).toURL

  override def publish(artifact: Artifact, credentials: (Username, Password))(implicit workingDir: Path): Try[Unit] = {
    val publisher = new SonatypePublisher(
      uri,
      snapshotUri,
      credentials = credentials._1 + ":" + credentials._2,
      gpgPassphrase = None,
      signed = false
    )

    Pom.generatePom(artifact).flatMap { pom =>
      publisher.publish(pom, jar = artifact.jar, artifact, release = !artifact.isSnapshot)
    }
  }
}


object NexusRepository {
  type Username = String
  type Password = String
}
