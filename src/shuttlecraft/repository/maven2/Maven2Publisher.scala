package shuttlecraft.repository.maven2

import java.nio.file.Path

import scalaj.http.HttpResponse
import shuttlecraft.Artifact
import scala.util.{Failure, Success, Try}
import shuttlecraft.progress._

class Maven2Publisher(api: Maven2HttpApi, resourceFactory: Maven2ResourceFactory) {

  def publish(artifact: Artifact)(implicit workingDir: Path): Try[Unit] = {
    artifact.progress("Generating derived resources...")
    for{
      allResources <- resourceFactory.prepareAllResources(artifact)
      _ = artifact.progress("Uploading resources...")
      publishResults <- api.uploadAll(allResources.filterNot{res =>
        //FIXME see https://stackoverflow.com/a/34020634
        api.repositoryUrl.contains("artifactory") && Set(".md5", ".sha1").exists(res.locator.endsWith)
      })
      _ <- reportPublishResults(publishResults, artifact)
    } yield ()
  }

  private def reportPublishResults(publishResults: Seq[HttpResponse[String]],
                                   artifact: Artifact): Try[Unit] = {
    if (publishResults.forall(_.is2xx)) {
      Success {artifact.progress(s"Published to ${api.repositoryUrl}")}
    } else {
      val errors = publishResults.filterNot(_.is2xx).map { response =>
        s"Code: ${response.code}, message: ${response.body}"
      }
      Failure(new RuntimeException(
        s"Failed to publish ${artifact.artifactId} to ${api.repositoryUrl}. Errors: \n${errors.mkString("\n")}"
      ))
    }
  }

}
