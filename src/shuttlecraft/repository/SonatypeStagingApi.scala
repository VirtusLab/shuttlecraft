package shuttlecraft.repository

import scalaj.http.{HttpOptions, HttpRequest, HttpResponse}
import shuttlecraft.http.HttpHeaders._
import shuttlecraft.http.PatientHttp

// Based on https://github.com/lihaoyi/mill

class SonatypeStagingApi(val baseUri: String, username: String, password: String) {
  //FIXME test if this still works at all

  // https://oss.sonatype.org/nexus-staging-plugin/default/docs/path__staging_profiles.html
  def getStagingProfileUri(groupId: String): String = {
    val response = withRetry(
      PatientHttp(s"$baseUri/staging/profiles").withJson.withBasicAuth(username, password))
        .throwError

    val resourceUri =
      ujson
        .read(response.body)("data")
        .arr
        .find(profile =>
          groupId.split('.').startsWith(profile("name").str.split('.')))
        .map(_("resourceURI").str.toString)

    resourceUri.getOrElse(
      throw new RuntimeException(
        s"Could not find staging profile for groupId: ${groupId}")
    )
  }

  def getStagingRepoState(stagingRepoId: String): String = {
    val response = PatientHttp(s"${baseUri}/staging/repository/${stagingRepoId}")
      .option(HttpOptions.readTimeout(60000))
      .withJson.withBasicAuth(username, password)
      .asString
      .throwError

    ujson.read(response.body)("type").str.toString
  }

  // https://oss.sonatype.org/nexus-staging-plugin/default/docs/path__staging_profiles_-profileIdKey-_start.html
  def createStagingRepo(profileUri: String, groupId: String): String = {
    val response = withRetry(PatientHttp(s"${profileUri}/start")
      .withJson.withBasicAuth(username, password)
      .postData(
        s"""{"data": {"description": "fresh staging profile for ${groupId}"}}"""))
      .throwError

    ujson.read(response.body)("data")("stagedRepositoryId").str.toString
  }

  // https://oss.sonatype.org/nexus-staging-plugin/default/docs/path__staging_profiles_-profileIdKey-_finish.html
  def closeStagingRepo(profileUri: String, repositoryId: String): Boolean = {
    val response = withRetry(
      PatientHttp(s"${profileUri}/finish")
        .withJson.withBasicAuth(username, password)
        .postData(
          s"""{"data": {"stagedRepositoryId": "${repositoryId}", "description": "closing staging repository"}}"""
        ))

    response.code == 201
  }

  // https://oss.sonatype.org/nexus-staging-plugin/default/docs/path__staging_profiles_-profileIdKey-_promote.html
  def promoteStagingRepo(profileUri: String, repositoryId: String): Boolean = {
    val response = withRetry(
      PatientHttp(s"${profileUri}/promote")
        .withJson.withBasicAuth(username, password)
        .postData(
          s"""{"data": {"stagedRepositoryId": "${repositoryId}", "description": "promote staging repository"}}"""
        ))

    response.code == 201
  }

  // https://oss.sonatype.org/nexus-staging-plugin/default/docs/path__staging_profiles_-profileIdKey-_drop.html
  def dropStagingRepo(profileUri: String, repositoryId: String): Boolean = {
    val response = withRetry(
      PatientHttp(s"${profileUri}/drop")
        .withJson.withBasicAuth(username, password)
        .postData(
          s"""{"data": {"stagedRepositoryId": "${repositoryId}", "description": "drop staging repository"}}"""
        ))

    response.code == 201
  }

  private def withRetry(request: HttpRequest,
                        retries: Int = 10): HttpResponse[String] = {
    val resp = request.asString
    if (resp.is5xx && retries > 0) {
      Thread.sleep(500)
      withRetry(request, retries - 1)
    } else {
      resp
    }
  }

}
