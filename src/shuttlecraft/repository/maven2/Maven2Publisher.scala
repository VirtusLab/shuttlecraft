package shuttlecraft.repository.maven2

import java.nio.file.Path

import scalaj.http.HttpResponse
import shuttlecraft.Artifact
import com.typesafe.scalalogging.Logger
import scala.util.{Failure, Success, Try}

class Maven2Publisher(api: Maven2HttpApi, resourceFactory: Maven2ResourceFactory) {

  val log = Logger[Maven2Publisher]

  def publish(artifact: Artifact)(implicit workingDir: Path): Try[Unit] = {
    for{
      allResources <- resourceFactory.prepareAllResources(artifact)
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
