package fdswarm.util

import munit.FunSuite
import io.circe.parser.decode
import java.time.Instant
import JavaTimeCirce.given

class JavaTimeCirceTest extends FunSuite:

  test("Instant decoder should handle ISO-8601 strings"):
    val isoString = "\"2026-03-03T23:12:38.148326Z\""
    val decoded = decode[Instant](isoString)
    assert(decoded.isRight, s"Failed to decode ISO string: ${decoded.left.map(_.getMessage)}")
    assertEquals(decoded.toOption.get, Instant.parse("2026-03-03T23:12:38.148326Z"))

  test("Instant decoder should handle Base64 encoded epoch seconds"):
    val instant = Instant.ofEpochSecond(1741043558L) // Some epoch
    val bb = java.nio.ByteBuffer.allocate(8)
    bb.putLong(instant.getEpochSecond)
    val base64 = java.util.Base64.getUrlEncoder.withoutPadding().encodeToString(bb.array())
    val json = s"\"$base64\""
    
    val decoded = decode[Instant](json)
    assert(decoded.isRight, s"Failed to decode Base64: ${decoded.left.map(_.getMessage)}")
    assertEquals(decoded.toOption.get, instant)
