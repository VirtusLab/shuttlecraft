/*
  Fury, version 0.4.0. Copyright 2018-19 Jon Pretty, Propensive Ltd.

  The primary distribution site is: https://propensive.com/

  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
  in compliance with the License. You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required  by applicable  law or  agreed to  in writing,  software  distributed  under the
  License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
  express  or  implied.  See  the  License for  the specific  language  governing  permissions and
  limitations under the License.
 */
package shuttlecraft

import java.io.File
import java.nio.file.Path

import scala.collection.SortedSet
import scala.util._
import scala.util.control.NonFatal

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
