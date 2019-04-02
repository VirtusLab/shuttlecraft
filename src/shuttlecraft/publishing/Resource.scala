package shuttlecraft.publishing

import java.nio.file.{Files, Path}

import scala.util.Try

sealed trait Resource{
  val locator: String
  val content: Array[Byte]

  def save(storage: Path): Try[FileResource] = {
    Try{
      Files.write(storage, content)
    }.map(FileResource(locator, _))
  }
}

case class InMemoryResource(locator: String, content: Array[Byte]) extends Resource

case class FileResource(locator: String, storage: Path) extends Resource{
  //TODO handle possible exceptions
  override lazy val content: Array[Byte] = Files.readAllBytes(storage)
}
