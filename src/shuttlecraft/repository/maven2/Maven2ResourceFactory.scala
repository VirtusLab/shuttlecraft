package shuttlecraft.repository.maven2

import java.nio.file.Path

import mercator._
import shuttlecraft._
import shuttlecraft.publishing.{FileResource, Resource}

import scala.util.Try

class Maven2ResourceFactory(
                       gpgPassphrase: Option[String],
                       signed: Boolean
                     ) {

  /**
    * Prepares just POM and JAR (assuming that the JAR file already exists in the filesystem
    * @param artifact
    * @param workingDir
    * @return
    */
  def prepareBasicResources(artifact: Artifact)(implicit workingDir: Path): Try[Seq[Resource]] = {
    Pom.generatePom(artifact).map { pom =>
      import artifact._
      val publishPath: String = Seq(
        groupId.replace(".", "/"),
        artifactId,
        version
      ).mkString("/")
      Seq(
        FileResource(locator = s"$publishPath/$artifactId-$version.pom", storage = pom),
        FileResource(locator = s"$publishPath/$artifactId-$version.jar", storage = jar)
      )
    }
  }

  /**
    * Prepares all resources, including signatures (if applicable) and checksums
    * @param artifact
    * @param workingDir
    * @return
    */
  def prepareAllResources(artifact: Artifact)(implicit workingDir: Path): Try[Seq[Resource]] = {
    for{
      basicResources <- prepareBasicResources(artifact)
      signatures <- if (signed) signatures(basicResources) else Try(Seq.empty)
      checksums = (basicResources ++ signatures).flatMap(Checksums.checksums)
    } yield basicResources ++ signatures ++ checksums
  }

  private def signatures(resources: Seq[Resource])(implicit workingDir: Path): Try[Seq[Resource]] = {
    val gpg = new Gpg(gpgPassphrase)
    resources.traverse(gpg.signature(_))
  }

}
