package shuttlecraft.repository.maven2

import java.io.File
import java.nio.file.Path

import shuttlecraft.{Artifact, ArtifactId, GroupId, Version}

import scala.collection.SortedSet
import scala.util.control.NonFatal
import scala.util.{Failure, Try}

object Pom {

  private def toXml(groupId: GroupId, artifactId: ArtifactId, version: Version): String = {
    s"""        <dependency>
       |            <groupId>${groupId}</groupId>
       |            <artifactId>${artifactId}</artifactId>
       |            <version>${version}</version>
       |            <scope>compile</scope>
       |        </dependency>
       |""".stripMargin
  }

  private def write(destination: Path,
           groupId: GroupId, artifactId: ArtifactId, version: Version,
           dependencies: Set[(GroupId, ArtifactId, Version)]): Try[Unit] = {
    val content: String =
      s"""<project xmlns="http://maven.apache.org/POM/4.0.0"
         |         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         |         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
         |    <modelVersion>4.0.0</modelVersion>
         |    <groupId>${groupId}</groupId>
         |    <artifactId>${artifactId}</artifactId>
         |    <version>${version}</version>
         |    <packaging>jar</packaging>
         |    <dependencies>
         |${dependencies.to[SortedSet].map((toXml _).tupled).mkString("")}
         |    </dependencies>
         |</project>
       """.stripMargin

    val p = new java.io.PrintWriter(destination.toFile)
    Try{
      p.print(content)
      p.close()
    }.recoverWith{
      case NonFatal(e) =>
        p.close()
        Failure(e)
    }

  }

  def generatePom(artifact: Artifact)(implicit workingDir: Path): Try[Path] = {
    val pom = File.createTempFile(artifact.artifactId, ".tmp.pom", workingDir.toFile)
    pom.deleteOnExit()
    Pom.write(
      destination = pom.toPath,
      artifact.groupId, artifact.artifactId, artifact.version,
      dependencies = artifact.dependencies
    ).map { _ => pom.toPath }
  }

}
