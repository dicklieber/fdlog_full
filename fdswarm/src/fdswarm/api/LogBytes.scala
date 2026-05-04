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

import fdswarm.logging.LogEventFieldNames

import java.nio.charset.StandardCharsets
import java.time.{Instant, OffsetDateTime}
import java.time.format.DateTimeFormatter
import scala.util.Try

object LogBytes:
  def apply(logPath: os.Path, sendNewer: Option[Instant]): Array[Byte] =
    sendNewer match
      case Some(cutoff) =>
        val lines = os.read.lines(logPath)
        linesBeginningAfter(lines, cutoff)
          .getOrElse(lines)
          .mkString("\n")
          .getBytes(StandardCharsets.UTF_8)
      case None =>
        os.read.bytes(logPath)

  private val timestampAtLineStart =
    raw"""^\{"${LogEventFieldNames.Timestamp}":"([^"]+)".*""".r

  private val logTimestampFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")

  private def linesBeginningAfter(lines: Seq[String], cutoff: Instant): Option[Seq[String]] =
    var firstNewerIndex = Option.empty[Int]

    lines.zipWithIndex.reverseIterator.foreach: (line, index) =>
      timestampAtStart(line).foreach: timestamp =>
        if timestamp.isAfter(cutoff) then
          firstNewerIndex = Some(index)
        else if firstNewerIndex.isDefined then
          return firstNewerIndex.map(lines.drop)

    firstNewerIndex.map(lines.drop)

  private def timestampAtStart(line: String): Option[Instant] =
    line match
      case timestampAtLineStart(timestamp) =>
        Try(OffsetDateTime.parse(timestamp, logTimestampFormatter).toInstant)
          .orElse(Try(Instant.parse(timestamp)))
          .toOption
      case _ =>
        None
