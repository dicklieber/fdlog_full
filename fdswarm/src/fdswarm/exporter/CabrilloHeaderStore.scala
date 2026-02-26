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

package fdswarm.exporter

import fdswarm.io.DirectoryProvider
import jakarta.inject.{Inject, Singleton}
import _root_.io.circe.Printer
import _root_.io.circe.parser.decode
import _root_.io.circe.syntax.*
import scalafx.beans.property.ObjectProperty

@Singleton
final class CabrilloHeaderStore @Inject() (directoryProvider: DirectoryProvider) {

  private val headerFile: os.Path =
    directoryProvider() / "cabrillo-header.json"

  /** Observable current header. */
  val header: ObjectProperty[CabrilloHeader] =
    ObjectProperty[CabrilloHeader](this, "header", loadOrDefault())

  /** Persist current header value to cabrillo-header.json */
  def save(): Unit =
    saveToDisk(header.value)

  /** Replace header (fires change listeners) + persist */
  def update(newHeader: CabrilloHeader): Unit = {
    header.value = newHeader
    saveToDisk(newHeader)
  }

  // ---------- internals ----------

  private def loadOrDefault(): CabrilloHeader =
    if os.exists(headerFile) then
      decode[CabrilloHeader](os.read(headerFile)) match
        case Right(h) => h
        case Left(_)  => CabrilloHeader()
    else
      CabrilloHeader()

  private val printer: Printer = Printer.spaces2.copy(dropNullValues = true)

  private def saveToDisk(h: CabrilloHeader): Unit = {
    os.makeDir.all(headerFile / os.up)
    os.write.over(headerFile, printer.print(h.asJson))
  }
}
