package shuttlecraft

import java.math.BigInteger
import java.security.MessageDigest

import shuttlecraft.publishing.{InMemoryResource, Resource}

object Checksums {

  def md5hex(bytes: Array[Byte]): Array[Byte] =
    hexArray(md5.digest(bytes)).getBytes

  def sha1hex(bytes: Array[Byte]): Array[Byte] =
    hexArray(sha1.digest(bytes)).getBytes

  def checksums(resource: Resource): Seq[Resource] = {
    import resource._
    Seq(
      (locator + ".md5") -> md5hex(content),
      (locator + ".sha1") -> sha1hex(content)
    ).map((InMemoryResource.apply _).tupled)
  }

  private def md5 = MessageDigest.getInstance("md5")

  private def sha1 = MessageDigest.getInstance("sha1")

  private def hexArray(arr: Array[Byte]) =
    String.format("%0" + (arr.length << 1) + "x", new BigInteger(1, arr))

}
