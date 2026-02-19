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

package fdswarm

import com.typesafe.scalalogging.LazyLogging
import fdswarm.io.DirectoryProvider
import fdswarm.model.{Callsign, Station}
import jakarta.inject.{Inject, Singleton}
import _root_.io.circe.Printer
import _root_.io.circe.parser.decode
import _root_.io.circe.syntax.*
import scalafx.beans.property.ObjectProperty
import scalafx.event.subscriptions.Subscription

@Singleton
final class StationManager @Inject()(
                                      productionDirectory: DirectoryProvider
                                    ) extends LazyLogging:

  private val file: os.Path =
    productionDirectory() / "station.json"
  /**
   * Observable current station.
   * None means "no station configured yet" or "failed to load".
   */
  val stationProperty: ObjectProperty[Station] =
    ObjectProperty(load())

  def station: Station =
    stationProperty.value

  def setStation(newStation: Station): Unit =
    stationProperty.value = newStation
    persist()



  // ---- persistence ----------------------------------------------------------

  private val printer: Printer = Printer.spaces2.copy(dropNullValues = true)

  private def persist(): Unit =
    val json = printer.print(station.asJson)
    os.write.over(file, json, createFolders = true)

  private def load(): Station =
    try {
      if !os.exists(file) then return Station("", "", Callsign(""))
      val sJson = os.read(file)
      decode[Station](sJson) match
        case Right(st) => st
        case Left(error) =>
          logger.error(s"Failed to decode Station from $file: ${error.getMessage}")
          Station("", "", Callsign(""))
    }
    catch
      case e: Throwable =>
        logger.warn(s"Failed to load station from $file: ${e.getMessage}")
        Station("", "", Callsign(""))

/*
  def pane(): Pane =
    logger.debug(s"Creating station form for $station")
    val myCaseForm = MyCaseForm[Station](station)
    logger.trace("New station result will be handled by container")
    myCaseForm.pane()*/
