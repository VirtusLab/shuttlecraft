package shuttlecraft.repository

import scalaj.http.HttpResponse
import shuttlecraft._
import shuttlecraft.publishing.Resource
import shuttlecraft.repository.maven2.{Maven2HttpApi, Maven2ResourceFactory}
import com.typesafe.scalalogging.Logger

// Based on https://github.com/lihaoyi/mill

class SonatypeReleasePublisher(val baseUri: String, username: String, password: String, resourceFactory: Maven2ResourceFactory) {

  val log = Logger[SonatypeReleasePublisher]

  private def publishRelease(release: Boolean,
                             payloads: Seq[Resource],
                             stagingProfile: String,
                             artifacts: Seq[Artifact]): Unit = {

    val stagingApi = new SonatypeStagingApi(baseUri, username, password)

    val profileUri = stagingApi.getStagingProfileUri(stagingProfile)
    val stagingRepoId =
      stagingApi.createStagingRepo(profileUri, stagingProfile)
    val releaseUri = s"${stagingApi.baseUri}/staging/deployByRepositoryId/$stagingRepoId/"

    val releaseApi = new Maven2HttpApi(releaseUri, username, password)

    val publishResults = payloads.map{resource =>
      log.info(s"Uploading ${resource.locator}")
      releaseApi.upload(resource)
    }
    reportPublishResults(publishResults, artifacts)

    if (release) {
      log.info("Closing staging repository")
      stagingApi.closeStagingRepo(profileUri, stagingRepoId)

      log.info("Waiting for staging repository to close")
      awaitRepoStatus("closed", stagingApi, stagingRepoId)

      log.info("Promoting staging repository")
      stagingApi.promoteStagingRepo(profileUri, stagingRepoId)

      log.info("Waiting for staging repository to release")
      awaitRepoStatus("released", stagingApi, stagingRepoId)

      log.info("Dropping staging repository")
      stagingApi.dropStagingRepo(profileUri, stagingRepoId)

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
                              stagingApi: SonatypeStagingApi,
                              stagingRepoId: String,
                              attempts: Int = 20): Unit = {
    def isRightStatus =
      stagingApi.getStagingRepoState(stagingRepoId).equalsIgnoreCase(status)
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

}
