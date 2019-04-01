package shuttlecraft.repository

import java.io.File
import java.net.{URI, URL}
import java.nio.file.Path

import shuttlecraft._
import NexusRepository._

import scala.util.Try


class ArtifactoryRepository(val url: URL) extends Repository[(Username, Password)] {

  override def publish(artifact: Artifact, credentials: (Username, Password))(implicit workingDir: Path): Try[Unit] = {
    Pom.generatePom(artifact).flatMap { pom =>
      //curl -u myUser:myP455w0rd! -X PUT "http://localhost:8081/artifactory/my-repository/my/new/artifact/directory/file.txt" -T Desktop/myNewFile.txt
      //see https://www.jfrog.com/confluence/display/RTF/Artifactory+REST+API
      ???
    }

  }
}

object ArtifactoryRepository {
  type Username = String
  type Password = String

  def apply(address: String) = new ArtifactoryRepository(new URL(address))
}



