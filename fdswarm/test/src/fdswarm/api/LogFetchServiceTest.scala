package fdswarm.api

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import fdswarm.io.FileHelper
import munit.FunSuite
import org.http4s.implicits.*
import org.http4s.{Method, Request, Status}
import sttp.tapir.server.http4s.Http4sServerInterpreter

import java.nio.charset.StandardCharsets

class LogFetchServiceTest extends FunSuite:

  test("full fetch from zero returns complete log"):
    val text =
      """{"message":"first"}
        |{"message":"second"}
        |""".stripMargin
    val service = serviceFor(
      text
    )

    val result = service.fetch(
      0L
    ).toOption.get

    assertEquals(
      utf8(result.bytes),
      text
    )
    assertEquals(
      result.from,
      0L
    )
    assertEquals(
      result.to,
      byteLength(text)
    )
    assertEquals(
      result.size,
      byteLength(text)
    )
    assertEquals(
      result.truncated,
      false
    )

  test("incremental fetch from previous X-Log-To returns newer bytes"):
    val first =
      """{"message":"first"}
        |""".stripMargin
    val second =
      """{"message":"second"}
        |""".stripMargin
    val path = writeLog(
      first
    )
    val service = FileLogFetchService(
      path.toNIO
    )
    val firstResult = service.fetch(
      0L
    ).toOption.get

    os.write.append(
      path,
      second
    )

    val secondResult = service.fetch(
      firstResult.to
    ).toOption.get

    assertEquals(
      utf8(secondResult.bytes),
      second
    )
    assertEquals(
      secondResult.from,
      firstResult.to
    )
    assertEquals(
      secondResult.to,
      byteLength(first + second)
    )

  test("fromByte equal to file size returns empty bytes"):
    val text =
      """{"message":"first"}
        |""".stripMargin
    val service = serviceFor(
      text
    )
    val size = byteLength(
      text
    )

    val result = service.fetch(
      size
    ).toOption.get

    assertEquals(
      result.bytes.toSeq,
      Seq.empty[Byte]
    )
    assertEquals(
      result.from,
      size
    )
    assertEquals(
      result.to,
      size
    )
    assertEquals(
      result.size,
      size
    )

  test("fromByte greater than file size reports truncation"):
    val service = serviceFor(
      """{"message":"first"}
        |""".stripMargin
    )

    val error = service.fetch(
      1000L
    ).left.toOption.get

    assertEquals(
      error.truncated,
      true
    )
    assertEquals(
      error.from,
      1000L
    )

  test("incomplete trailing line is not returned"):
    val text =
      """{"message":"complete"}
        |{"message":"incomplete"}""".stripMargin
    val service = serviceFor(
      text
    )

    val result = service.fetch(
      0L
    ).toOption.get

    assertEquals(
      utf8(result.bytes),
      """{"message":"complete"}
        |""".stripMargin
    )

  test("X-Log-To advances only to the last complete newline"):
    val complete =
      """{"message":"complete"}
        |""".stripMargin
    val incomplete =
      """{"message":"incomplete"}"""
    val service = serviceFor(
      complete + incomplete
    )

    val result = service.fetch(
      0L
    ).toOption.get

    assertEquals(
      result.to,
      byteLength(complete)
    )
    assertEquals(
      result.size,
      byteLength(complete + incomplete)
    )

  test("negative fromByte returns an error"):
    val service = serviceFor(
      """{"message":"first"}
        |""".stripMargin
    )

    val error = service.fetch(
      -1L
    ).left.toOption.get

    assertEquals(
      error.truncated,
      false
    )
    assertEquals(
      error.from,
      -1L
    )

  test("negative fromByte returns 400 from the HTTP endpoint"):
    val root = os.temp.dir()
    os.write(
      root / "fdswarm.log",
      """{"message":"first"}
        |""".stripMargin
    )
    val endpoints = LogsEndpoints(
      new FileHelper:
        override val directory: os.Path = root
    ).endpoints
    val routes = Http4sServerInterpreter[IO]()
      .toRoutes(
        endpoints
      )
      .orNotFound

    val response = routes
      .run(
        Request[IO](
          method = Method.GET,
          uri = uri"/log?fromByte=-1"
        )
      )
      .unsafeRunSync()

    assertEquals(
      response.status,
      Status.BadRequest
    )

  private def serviceFor(text: String): LogFetchService =
    FileLogFetchService(
      writeLog(
        text
      ).toNIO
    )

  private def writeLog(text: String): os.Path =
    val root = os.temp.dir()
    val path = root / "fdswarm.log"
    os.write(
      path,
      text
    )
    path

  private def utf8(bytes: Array[Byte]): String =
    new String(
      bytes,
      StandardCharsets.UTF_8
    )

  private def byteLength(text: String): Long =
    text.getBytes(
      StandardCharsets.UTF_8
    ).length.toLong
