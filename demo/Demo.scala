package shuttlecraft

import java.nio.file.Paths

import shuttlecraft.repository.maven2._
import com.typesafe.scalalogging.Logger
import scala.util.control.NonFatal

object Demo {
  def main(args: Array[String]): Unit = {

    val log = Logger("main")

    implicit val dir = Paths.get(System.getProperty("fury.sharedDir"))

    val jar = Paths.get("target", "shuttlecraft-demo.jar")

    val artifact = Artifact(
      groupId = "com.example",
      artifactId = "shuttlecraft",
      version = "0.0.1-SNAPSHOT",
      author = None,
      license = None,
      dependencies = Set(
        ("com.lihaoyi","ujson_2.12","0.7.1")
      ),
      jar = jar
    )

    val mvnApi = new Maven2HttpApi(
      repositoryUrl = "http://localhost:8081/nexus/content/repositories/snapshots/",
      username = "admin",
      password = "admin123"
    )

    val resourceGen = new Maven2ResourceFactory(gpgPassphrase = None, signed = false)

    val publisher = new Maven2Publisher(mvnApi, resourceGen)

    publisher.publish(artifact).recover{ case NonFatal(e) => log.error("Failed to publish the artifact", e) }
  }
}
