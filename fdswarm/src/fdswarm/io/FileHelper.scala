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
class FileHelper extends fdswarm.DirectoryProvider:
  private lazy val logger = StructuredLogger("fileHelper")

  def loadOrDefault[T: Decoder](fileName: String)(default: => T): T =

    val path = apply() / fileName
    try
      val sJson = os.read(path)
      val r: T = parse(sJson).flatMap(_.as[T]).fold(
        err =>
          logger.error("Failed to parse/decode JSON", err, "File" -> fileName)
          default
        ,
        identity)
      r
    catch
      case e: Exception =>
        logger.error("Failed to read file", e, "File" -> fileName)
        default

  /** At testing time, the port is not available we just use home directory/fdswarm.
    * But for tests we provide a PORT environment variable.
    * @return
    */
  def apply(): os.Path =
    val base = os.home / "fdswarm"
    sys.env.get("PORT") match
      case Some(port) => base / port
      case None       => base

  def save[T: Encoder](fileName: String, value: T): Unit =
    val path = apply() / fileName
    val json = value.asJson.printWith(Printer.indented("  "))
    os.write.over(path, json, createFolders = true)
