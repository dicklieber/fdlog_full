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

import _root_.io.circe.Printer
import fdswarm.io.FileHelper
import jakarta.inject.{Inject, Singleton}
import scalafx.beans.property.ObjectProperty

@Singleton
final class CabrilloHeaderStore @Inject() (fileHelper: FileHelper):

  /** Observable current header. */
  val header: ObjectProperty[CabrilloHeader] =
    ObjectProperty[CabrilloHeader](this, "header", loadOrDefault())
  private lazy val file = "cabrillo-header.json"
  private val printer: Printer = Printer.spaces2.copy(dropNullValues = true)

//  /** Persist current header value to cabrillo-header.json */
//  def save(): Unit =
//    save(header.value)

  // ---------- internals ----------

  /** Replace header (fires change listeners) + persist */
  def update(newHeader: CabrilloHeader): Unit = {
    header.value = newHeader
    save(newHeader)
  }

  private def save(cabrilloHeader: CabrilloHeader): Unit =
    fileHelper.save(file, cabrilloHeader)

  private def loadOrDefault(): CabrilloHeader =
    fileHelper.loadOrDefault(file)(CabrilloHeader())
