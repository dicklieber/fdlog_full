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
import io.circe.parser.parse
import jakarta.inject.{Inject, Singleton}
import sttp.tapir.*
import sttp.tapir.server.ServerEndpoint

import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Locale

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
    val (since, acceptEncoding) = input
    IO.blocking {
      val logBytes = since match
        case Some(cutoff) =>
          os.read
            .lines(logPath)
            .filter(line => LogsEndpoints.lineIsSince(line, cutoff))
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
    fileHelper.directory / "logs" / "fdswarm.log"

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
      .in(query[Option[Instant]]("since"))
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

  private def lineIsSince(line: String, cutoff: Instant): Boolean =
    parse(line.trim)
      .flatMap(_.hcursor.get[String](LogEventFieldNames.Timestamp))
      .toOption
      .flatMap(timestamp => scala.util.Try(Instant.parse(timestamp)).toOption)
      .exists(timestamp => !timestamp.isBefore(cutoff))
