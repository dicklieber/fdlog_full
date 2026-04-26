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

import _root_.io.circe.Printer
import _root_.io.circe.parser.decode
import _root_.io.circe.syntax.*
import fdswarm.logging.LazyStructuredLogging
import fdswarm.fx.station.StationConfig
import fdswarm.io.FileHelper
import fdswarm.model.Callsign
import jakarta.inject.{Inject, Singleton}
import scalafx.beans.property.ObjectProperty

@Singleton
final class StationConfigManager @Inject()(
                                            fileHelper: FileHelper,
                                            startupInfo: StartupInfo
                                    ) extends LazyStructuredLogging:

  val stationProperty:ObjectProperty[StationConfig] =
    ObjectProperty(startupInfo.info match
      case Some(debugConfig) =>
        logger.info(s"Using debug config: $debugConfig")
        StationConfig(debugConfig.operator)
      case None =>
        logger.info("No debug config found, loading station from file")
        load()
    )

  def stationConfig: StationConfig =
    stationProperty.value

  def setStation(newStation: StationConfig): Unit =
    stationProperty.value = newStation
    persist()



  // ---- persistence ----------------------------------------------------------

  private val printer: Printer = Printer.spaces2.copy(dropNullValues = true)

  private def persist(): Unit =
    fileHelper.save("station.json", stationConfig)

  private def load(): StationConfig =
    fileHelper.loadOrDefault[StationConfig]("station.json")(StationConfig())
