/*
 * Copyright (c) 2026. Dick Lieber, WA9NNN
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package fdswarm.api

import cats.effect.IO
import fdswarm.io.FileHelper
import fdswarm.logging.LogEventFieldNames
import fdswarm.util.Gzip
import jakarta.inject.{Inject, Singleton}
import sttp.tapir.*
import sttp.tapir.server.ServerEndpoint

import java.nio.charset.StandardCharsets
import java.time.{Instant, OffsetDateTime}
import java.time.format.DateTimeFormatter
import java.util.Locale
import scala.util.Try

/** Tapir endpoints for downloading the current application log. */
@Singleton
final class LogsEndpoints @Inject() (fileHelper: FileHelper) extends ApiEndpoints:

  private val log: ServerEndpoint[Any, IO] =
    LogsEndpoints.logDef
      .serverLogicSuccess[IO](logResponse)

  override def endpoints: List[ServerEndpoint[Any, IO]] = List(
    log
  )

  private def logResponse(
      input: (Option[Instant], Option[String])
  ): IO[(String, String, Option[String], String, Array[Byte])] =
    val ( sendNewer, acceptEncoding) = input
    IO.blocking {
      val logBytes = sendNewer match
        case Some(cutoff) =>
          val lines = os.read.lines(logPath)
          LogsEndpoints
            .linesBeginningAfter(lines, cutoff)
            .getOrElse(lines)
            .mkString("\n")
            .getBytes(StandardCharsets.UTF_8)
        case None =>
          os.read.bytes(logPath)

      val (contentEncoding, bytes) =
        if LogsEndpoints.acceptsGzip(acceptEncoding) then
          Some("gzip") -> Gzip.compress(logBytes)
        else
          None -> logBytes

      (
        LogsEndpoints.contentDisposition,
        LogsEndpoints.contentType,
        contentEncoding,
        LogsEndpoints.vary,
        bytes
      )
    }

  private def logPath: os.Path =
    fileHelper.directory  / "fdswarm.log"

private object LogsEndpoints:
  private val logBody =
    header[String]("Content-Disposition")
      .and(header[String]("Content-Type"))
      .and(header[Option[String]]("Content-Encoding"))
      .and(header[String]("Vary"))
      .and(byteArrayBody)

  private val logDef: PublicEndpoint[
    (Option[Instant], Option[String]),
    Unit,
    (String, String, Option[String], String, Array[Byte]),
    Any
  ] =
    endpoint
      .get
      .in("log")
      .in(query[Option[Instant]]("sendNewer"))
      .in(header[Option[String]]("Accept-Encoding"))
      .out(logBody)
      .description("Download the current fdswarm.log file")

  private val contentDisposition =
    "attachment; filename=\"fdswarm.log\""

  private val contentType =
    "text/plain; charset=utf-8"

  private val vary =
    "Accept-Encoding"

  private def acceptsGzip(acceptEncoding: Option[String]): Boolean =
    acceptEncoding.exists(
      _.split(",")
        .iterator
        .map(_.trim)
        .exists { entry =>
          val parts = entry.split(";").map(_.trim)
          parts.headOption.exists(_.equalsIgnoreCase("gzip")) &&
            parts.drop(1).forall(part => !part.toLowerCase(Locale.ROOT).startsWith("q=0"))
        }
    )

  private val timestampAtLineStart =
    raw"""^\{"${LogEventFieldNames.Timestamp}":"([^"]+)"""".r

  private val logTimestampFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")

  private def linesBeginningAfter(lines: Seq[String], cutoff: Instant): Option[Seq[String]] =
    lines
      .zipWithIndex
      .collectFirst {
        case (line, index) if timestampAtStart(line).exists(_.isAfter(cutoff)) =>
          lines.drop(index)
      }

  private def timestampAtStart(line: String): Option[Instant] =
    line match
      case timestampAtLineStart(timestamp) =>
        Try(OffsetDateTime.parse(timestamp, logTimestampFormatter).toInstant)
          .orElse(Try(Instant.parse(timestamp)))
          .toOption
      case _ =>
        None
