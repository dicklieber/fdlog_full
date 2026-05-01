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

package fdswarm.io

import fdswarm.logging.StructuredLogger
import io.circe.parser.*
import io.circe.syntax.*
import io.circe.{Decoder, Encoder, Printer}

import java.nio.file.NoSuchFileException

/** A utility class for handling file-related operations, such as reading and writing JSON-encoded
  * data to files, and managing application-specific directory paths.
  */
class FileHelper:
  private lazy val logger = StructuredLogger("fileHelper")
  val directory: os.Path =
    val base = os.home / "fdswarm"
    sys.env.get("PORT") match
      case Some(port) =>
        base / port
      case None =>
        base

  def loadOrDefault[T: Decoder](fileName: String)(default: => T): T =

    val path = directory / fileName
    try
      val sJson: String = os.read(path)
      val r: T = parse(sJson).flatMap(_.as[T]).fold(
        err =>
          logger.error("Failed to parse/decode JSON", err, "File" -> fileName)
          default
        ,
        identity
      )
      r
    catch
      case _: NoSuchFileException =>
        logger.debug("File not found, using default", "File" -> fileName)
        default
      case e: Exception =>
        logger.error("Failed to read file", e, "File" -> fileName)
        default

  def save[T: Encoder](fileName: String, value: T): Unit =
    val path = directory / fileName
    val json = value.asJson.printWith(Printer.indented("  "))
    os.write.over(path, json, createFolders = true)

  def remove(fileName: String): Unit =
    val path = directory / fileName
    os.remove(path)
