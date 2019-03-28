import java.net.URL
import java.nio.file.Path

import scala.util.Try

package object shuttlecraft{

  type GroupId = String
  type ArtifactId = String
  type Version = String
  type Author = String
  type License = String
  type Jar = Path
  type Dependency = (GroupId, ArtifactId, Version)

  private implicit class VersionWithSnapshot(val version: Version) extends AnyVal{
    def isSnapshot: Boolean = version.endsWith("-SNAPSHOT")
  }

  case class Artifact(
                     groupId: GroupId,
                     artifactId: ArtifactId,
                     version: Version,
                     author: Option[Author],
                     license: Option[License],
                     dependencies: Set[Dependency],
                     jar: Jar
                     ) {
    def isSnapshot: Boolean = version.isSnapshot
  }

  trait Repository[Credentials]{
    val url: URL

    def publish(artifact: Artifact, credentials: Credentials)(implicit workingDir: Path): Try[Unit]
  }

}
