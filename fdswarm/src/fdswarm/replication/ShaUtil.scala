package fdswarm.replication

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

def calcShaHash(
    strings: IterableOnce[String]
  ): String =
  val messageDigest = MessageDigest.getInstance("SHA-512")

  // sort to make order irrelevant
  val sorted = strings.iterator.toSeq.sorted

  sorted.foreach { s =>
    val bytes = s.getBytes(StandardCharsets.UTF_8)
    val lenBytes = ByteBuffer.allocate(4).putInt(bytes.length).array()
    messageDigest.update(lenBytes) // preserves boundaries
    messageDigest.update(bytes)
  }

  messageDigest.digest().map("%02x".format(_)).mkString


