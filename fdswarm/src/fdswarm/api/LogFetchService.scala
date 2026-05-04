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

import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.StandardOpenOption.READ
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.{Files, Path}

case class LogFetchResult(
    from: Long,
    to: Long,
    size: Long,
    logId: String,
    truncated: Boolean,
    bytes: Array[Byte]
)

sealed trait LogFetchError:
  def from: Long
  def size: Long
  def logId: String
  def truncated: Boolean

object LogFetchError:
  case class NegativeFromByte(
      from: Long,
      size: Long,
      logId: String
  ) extends LogFetchError:
    override val truncated: Boolean = false

  case class FromBeyondFileSize(
      from: Long,
      size: Long,
      logId: String
  ) extends LogFetchError:
    override val truncated: Boolean = true

trait LogFetchService:
  def fetch(fromByte: Long): Either[LogFetchError, LogFetchResult]

final class FileLogFetchService(logPath: Path) extends LogFetchService:

  override def fetch(fromByte: Long): Either[LogFetchError, LogFetchResult] =
    val metadata = LogFileMetadata.fromPath(logPath)
    if fromByte < 0 then
      Left(
        LogFetchError.NegativeFromByte(
          from = fromByte,
          size = metadata.size,
          logId = metadata.logId
        )
      )
    else if fromByte > metadata.size then
      Left(
        LogFetchError.FromBeyondFileSize(
          from = fromByte,
          size = metadata.size,
          logId = metadata.logId
        )
      )
    else if fromByte == metadata.size then
      Right(
        LogFetchResult(
          from = fromByte,
          to = fromByte,
          size = metadata.size,
          logId = metadata.logId,
          truncated = false,
          bytes = new Array[Byte](0)
        )
      )
    else
      val length = Math.toIntExact(
        metadata.size - fromByte
      )
      val bytes = readBytes(
        from = fromByte,
        length = length
      )
      val completeLength = completeLineLength(
        bytes
      )
      val completeBytes =
        if completeLength == bytes.length then bytes
        else bytes.take(completeLength)

      Right(
        LogFetchResult(
          from = fromByte,
          to = fromByte + completeLength,
          size = metadata.size,
          logId = metadata.logId,
          truncated = false,
          bytes = completeBytes
        )
      )

  private def readBytes(
      from: Long,
      length: Int
  ): Array[Byte] =
    val bytes = new Array[Byte](length)
    val buffer = ByteBuffer.wrap(
      bytes
    )
    val channel = FileChannel.open(
      logPath,
      READ
    )
    try
      channel.position(
        from
      )
      while buffer.hasRemaining && channel.read(buffer) != -1 do ()
    finally
      channel.close()
    bytes

  private def completeLineLength(bytes: Array[Byte]): Int =
    bytes.lastIndexWhere(
      _ == '\n'.toByte
    ) match
      case -1    => 0
      case index => index + 1

private case class LogFileMetadata(
    size: Long,
    logId: String
)

private object LogFileMetadata:
  def fromPath(path: Path): LogFileMetadata =
    val attributes = Files.readAttributes(
      path,
      classOf[BasicFileAttributes]
    )
    LogFileMetadata(
      size = attributes.size(),
      logId = logId(
        path,
        attributes
      )
    )

  private def logId(
      path: Path,
      attributes: BasicFileAttributes
  ): String =
    Option(
      attributes.fileKey()
    ).map(fileKey =>
      s"${path.toAbsolutePath.normalize()}#$fileKey"
    ).getOrElse(
      path.toAbsolutePath.normalize().toString
    )
