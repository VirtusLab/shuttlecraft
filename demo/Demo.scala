package shuttlecraft

import java.nio.file.Paths

import repository._

object Demo {
  def main(args: Array[String]): Unit = {

    implicit val dir = Paths.get(System.getProperty("fury.sharedDir"))

    val jar = Paths.get("target", "shuttlecraft.jar")

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

    val repo = new NexusRepository(
      uri = "http://localhost:8082/repository/maven-releases/",
      snapshotUri = "http://localhost:8082/repository/maven-snapshots/"
    )

    repo.publish(artifact, credentials = "admin" -> "admin123")
  }
}
