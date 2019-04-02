package shuttlecraft.http

import java.util.Base64

import scalaj.http.HttpRequest

object HttpHeaders {

  implicit class BasicAuth(val httpRequest: HttpRequest) extends AnyVal{
    def withBasicAuth(username: String, password: String): HttpRequest = {
      val base64Creds = base64(username + ":" + password)
      httpRequest.header("Authorization", s"Basic $base64Creds")
    }
  }

  implicit class DataType(val httpRequest: HttpRequest) extends AnyVal{
    def withJson: HttpRequest = {
      httpRequest.headers(
        "Accept" -> "application/json",
        "Content-Type" -> "application/json"
      )
    }
    def withBinary: HttpRequest = {
      httpRequest.header("Content-Type", "application/binary")
    }
  }

  private def base64(s: String) =
    new String(Base64.getEncoder.encode(s.getBytes))

}
