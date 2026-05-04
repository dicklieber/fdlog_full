package fdswarm.api

import munit.FunSuite

import java.nio.charset.StandardCharsets
import java.time.Instant

class LogBytesTest extends FunSuite:
  test("returns complete log bytes when sendNewer is empty"):
    val root = os.temp.dir()
    val path = root / "fdswarm.log"
    val text =
      """plain prefix
        |{"@timestamp":"2026-01-01T00:00:00.000+0000","message":"first"}
        |trailing line
        |""".stripMargin
    os.write(
      path,
      text
    )

    val bytes = LogBytes(
      path,
      None
    )

    assertEquals(
      bytes.toSeq,
      text.getBytes(StandardCharsets.UTF_8).toSeq
    )

  test("returns lines beginning with the first log timestamp after sendNewer"):
    val path = writeLog(
      Seq(
        """{"@timestamp":"2026-01-01T00:00:00.000+0000","message":"old"}""",
        """untimestamped line belonging to old event""",
        """{"@timestamp":"2026-01-01T00:00:02.000+0000","message":"new"}""",
        """new event continuation"""
      )
    )

    val bytes = LogBytes(
      path,
      Some(
        Instant.parse(
          "2026-01-01T00:00:01Z"
        )
      )
    )

    assertEquals(
      new String(
        bytes,
        StandardCharsets.UTF_8
      ),
      """{"@timestamp":"2026-01-01T00:00:02.000+0000","message":"new"}
        |new event continuation""".stripMargin
    )

  test("keeps all newer timestamped events when searching from the end"):
    val path = writeLog(
      Seq(
        """{"@timestamp":"2026-01-01T00:00:00.000+0000","message":"old"}""",
        """old event continuation""",
        """{"@timestamp":"2026-01-01T00:00:02.000+0000","message":"newer"}""",
        """newer event continuation""",
        """{"@timestamp":"2026-01-01T00:00:03.000+0000","message":"newest"}""",
        """newest event continuation"""
      )
    )

    val bytes = LogBytes(
      path,
      Some(
        Instant.parse(
          "2026-01-01T00:00:01Z"
        )
      )
    )

    assertEquals(
      new String(
        bytes,
        StandardCharsets.UTF_8
      ),
      """{"@timestamp":"2026-01-01T00:00:02.000+0000","message":"newer"}
        |newer event continuation
        |{"@timestamp":"2026-01-01T00:00:03.000+0000","message":"newest"}
        |newest event continuation""".stripMargin
    )

  test("falls back to the complete log when no timestamp is newer"):
    val lines = Seq(
      """{"@timestamp":"2026-01-01T00:00:00.000+0000","message":"old"}""",
      """{"@timestamp":"2026-01-01T00:00:01Z","message":"also old"}"""
    )
    val path = writeLog(
      lines
    )

    val bytes = LogBytes(
      path,
      Some(
        Instant.parse(
          "2026-01-01T00:00:02Z"
        )
      )
    )

    assertEquals(
      new String(
        bytes,
        StandardCharsets.UTF_8
      ),
      lines.mkString(
        "\n"
      )
    )

  private def writeLog(lines: Seq[String]): os.Path =
    val root = os.temp.dir()
    val path = root / "fdswarm.log"
    os.write(
      path,
      lines.mkString(
        "\n"
      )
    )
    path
