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
import fdswarm.model.Station
import jakarta.inject.{Inject, Singleton}
import scalafx.beans.property.ObjectProperty
import scalafx.event.subscriptions.Subscription
import upickle.default.*

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

  private def persist(): Unit =
    val json = write(station, indent = 2)
    os.write.over(file, json, createFolders = true)

  private def load(): Station =
    try {
      val sJson = os.read(file)
      read[Station](sJson)
    }
    catch
      case e: Throwable =>
        logger.warn(s"Failed to load station from $file: ${e.getMessage}")
        Station("", "", "")

/*
  def pane(): Pane =
    logger.info(s"Creating station form for $station")
    val myCaseForm = MyCaseForm[Station](station)
    logger.trace("New station result will be handled by container")
    myCaseForm.pane()*/
