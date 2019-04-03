package shuttlecraft.repository.maven2

import scalaj.http.{HttpOptions, HttpResponse}
import shuttlecraft.http.HttpHeaders._
import shuttlecraft.http.PatientHttp
import shuttlecraft.publishing.Resource
import com.typesafe.scalalogging.Logger
import scala.concurrent.duration._
import scala.util.Try

class Maven2HttpApi(val repositoryUrl: String, username: String, password: String) {

  val log = Logger[Maven2HttpApi]

  private val uploadTimeout = 5.minutes.toMillis.toInt

  def upload(resource: Resource): HttpResponse[String] = upload(resource.locator, resource.content)

  def uploadAll(resources: Seq[Resource]): Try[Seq[HttpResponse[String]]] = Try {
    resources.map { resource =>
      log.info(s"Uploading ${resource.locator}")
      upload(resource)
    }
  }

  private def upload(uploadPath: String, data: Array[Byte]): HttpResponse[String] = {
    PatientHttp(repositoryUrl + "/" + uploadPath)
      .option(HttpOptions.readTimeout(uploadTimeout))
      .withBinary.withBasicAuth(username, password)
      .put(data)
      .asString
  }

}
