package shuttlecraft.repository.maven2

import java.nio.file.Path

import scalaj.http.HttpResponse
import shuttlecraft.Artifact

import scala.util.{Failure, Success, Try}

class Maven2Publisher(api: Maven2HttpApi, resourceFactory: Maven2ResourceFactory) {

  object log {
    def info(msg: String) = println(msg)
  }

  def publish(artifact: Artifact)(implicit workingDir: Path): Try[Unit] = {
    for{
      allResources <- resourceFactory.prepareAllResources(artifact)
      publishResults <- api.uploadAll(allResources)
      _ <- reportPublishResults(publishResults, artifact)
    } yield ()
  }

  private def reportPublishResults(publishResults: Seq[HttpResponse[String]],
                                   artifact: Artifact): Try[Unit] = {
    if (publishResults.forall(_.is2xx)) {
      Success {log.info(s"Published ${artifact.artifactId} to ${api.repositoryUrl}")}
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
