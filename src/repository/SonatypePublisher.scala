package repository

import java.math.BigInteger
import java.nio.file.{Files, Path, Paths}
import java.security.MessageDigest

import shuttlecraft._
import guillotine._
import scalaj.http.HttpResponse

import scala.util.Try

// Based on https://github.com/lihaoyi/mill

object log{
  def info(msg: String) = println(msg)
}

class SonatypePublisher(uri: String,
                        snapshotUri: String,
                        credentials: String,
                        gpgPassphrase: Option[String],
                        signed: Boolean
                       ) {

  private val api = new SonatypeHttpApi(uri, credentials)

  def publish(pom: Path, jar: Path, artifact: Artifact, release: Boolean): Try[Unit] = {
    val name = artifact.artifactId + "-" + artifact.version
    Try{
      publish(Seq(pom -> s"$name.pom", jar -> s"$name.jar"), artifact, release)
    }
  }

  def publish(fileMapping: Seq[(Path, String)], artifact: Artifact, release: Boolean): Unit = {
    publishAll(release, fileMapping -> artifact)
  }
  def publishAll(release: Boolean, artifacts: (Seq[(Path, String)], Artifact)*): Unit = {

    val mappings = for ((fileMapping0, artifact) <- artifacts) yield {
      val publishPath = Seq(
        artifact.groupId.replace(".", "/"),
        artifact.artifactId,
        artifact.version
      ).mkString("/")
      val fileMapping = fileMapping0.map{ case (file, name) => (file, Paths.get(publishPath, name))}

      val signedArtifacts = if (signed) fileMapping.map {
        case (file, name) => poorMansSign(file, gpgPassphrase) -> Paths.get(name.toString + ".asc")
      } else Seq()

      artifact -> (fileMapping ++ signedArtifacts).flatMap {
        case (file, name) =>
          val content = Files.readAllBytes(file)

          Seq(
            name.toString -> content,
            (name.toString + ".md5") -> md5hex(content),
            (name.toString + ".sha1") -> sha1hex(content)
          )
      }
    }

    val (snapshots, releases) = mappings.partition(_._1.isSnapshot)
    if(snapshots.nonEmpty) {
      publishSnapshot(snapshots.flatMap(_._2), snapshots.map(_._1))
    }
    val releaseGroups = releases.groupBy(_._1.groupId)
    for((group, groupReleases) <- releaseGroups){
      publishRelease(release, groupReleases.flatMap(_._2), group, releases.map(_._1))
    }
  }

  private def publishSnapshot(payloads: Seq[(String, Array[Byte])],
                              artifacts: Seq[Artifact]): Unit = {

    val publishResults = payloads.map {
      case (fileName, data) =>
        log.info(s"Uploading $fileName")
        val resp = api.upload(s"$snapshotUri/$fileName", data)
        resp
    }
    reportPublishResults(publishResults, artifacts)
  }

  private def publishRelease(release: Boolean,
                             payloads: Seq[(String, Array[Byte])],
                             stagingProfile: String,
                             artifacts: Seq[Artifact]): Unit = {
    val profileUri = api.getStagingProfileUri(stagingProfile)
    val stagingRepoId =
      api.createStagingRepo(profileUri, stagingProfile)
    val baseUri = s"$uri/staging/deployByRepositoryId/$stagingRepoId/"

    val publishResults = payloads.map {
      case (fileName, data) =>
        log.info(s"Uploading ${fileName}")
        api.upload(s"$baseUri/$fileName", data)
    }
    reportPublishResults(publishResults, artifacts)

    if (release) {
      log.info("Closing staging repository")
      api.closeStagingRepo(profileUri, stagingRepoId)

      log.info("Waiting for staging repository to close")
      awaitRepoStatus("closed", stagingRepoId)

      log.info("Promoting staging repository")
      api.promoteStagingRepo(profileUri, stagingRepoId)

      log.info("Waiting for staging repository to release")
      awaitRepoStatus("released", stagingRepoId)

      log.info("Dropping staging repository")
      api.dropStagingRepo(profileUri, stagingRepoId)

      log.info(s"Published ${artifacts.map(_.artifactId).mkString(", ")} successfully")
    }
  }

  private def reportPublishResults(publishResults: Seq[HttpResponse[String]],
                                   artifacts: Seq[Artifact]) = {
    if (publishResults.forall(_.is2xx)) {
      log.info(s"Published ${artifacts.map(_.artifactId).mkString(", ")} to Sonatype")
    } else {
      val errors = publishResults.filterNot(_.is2xx).map { response =>
        s"Code: ${response.code}, message: ${response.body}"
      }
      throw new RuntimeException(
        s"Failed to publish ${artifacts.map(_.artifactId).mkString(", ")} to Sonatype. Errors: \n${errors.mkString("\n")}"
      )
    }
  }

  private def awaitRepoStatus(status: String,
                              stagingRepoId: String,
                              attempts: Int = 20): Unit = {
    def isRightStatus =
      api.getStagingRepoState(stagingRepoId).equalsIgnoreCase(status)
    var attemptsLeft = attempts

    while (attemptsLeft > 0 && !isRightStatus) {
      Thread.sleep(3000)
      attemptsLeft -= 1
      if (attemptsLeft == 0) {
        throw new RuntimeException(
          s"Couldn't wait for staging repository to be ${status}. Failing")
      }
    }
  }

  // http://central.sonatype.org/pages/working-with-pgp-signatures.html#signing-a-file
  private def poorMansSign(file: Path, maybePassphrase: Option[String]): Path = {
    import guillotine.environments.none
    val fileName = file.toString
    val cmd = maybePassphrase match {
      case Some(passphrase) =>
        sh"gpg --passphrase $passphrase --batch --yes -a -b $fileName"
      case None =>
        sh"gpg --batch --yes -a -b $fileName"
    }
    cmd.exec[String]
    Paths.get(fileName + ".asc")
  }

  private def md5hex(bytes: Array[Byte]): Array[Byte] =
    hexArray(md5.digest(bytes)).getBytes

  private def sha1hex(bytes: Array[Byte]): Array[Byte] =
    hexArray(sha1.digest(bytes)).getBytes

  private def md5 = MessageDigest.getInstance("md5")

  private def sha1 = MessageDigest.getInstance("sha1")

  private def hexArray(arr: Array[Byte]) =
    String.format("%0" + (arr.length << 1) + "x", new BigInteger(1, arr))

}
