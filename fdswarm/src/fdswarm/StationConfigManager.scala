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
import fdswarm.model.Callsign
import jakarta.inject.{Inject, Singleton}
import scalafx.beans.property.ObjectProperty

@Singleton
final class StationConfigManager @Inject()(
                                      productionDirectory: fdswarm.DirectoryProvider,
                                      startupInfo: StartupInfo
                                    ) extends LazyStructuredLogging:

  private val file: os.Path =
    productionDirectory() / "station.json"

  val stationProperty:ObjectProperty[StationConfig] =
    ObjectProperty(startupInfo.info match
      case Some(debugConfig) =>
        logger.info(s"Using debug config: $debugConfig")
        StationConfig(debugConfig.operator)
      case None =>
        logger.info("No debug config found, loading station from file")
        load()
    )

  def station: StationConfig =
    stationProperty.value

  def setStation(newStation: StationConfig): Unit =
    stationProperty.value = newStation
    persist()



  // ---- persistence ----------------------------------------------------------

  private val printer: Printer = Printer.spaces2.copy(dropNullValues = true)

  private def persist(): Unit =
    val json = printer.print(station.asJson)
    os.write.over(file, json, createFolders = true)

  private def load(): StationConfig =
    try {
      if !os.exists(file) then return StationConfig(Callsign(""), "", "")
      val sJson = os.read(file)
      decode[StationConfig](sJson) match
        case Right(st) => st
        case Left(error) =>
          logger.error(s"Failed to decode Station from $file: ${error.getMessage}")
          StationConfig(Callsign(""), "", "")
    }
    catch
      case e: Throwable =>
        logger.warn(s"Failed to load station from $file: ${e.getMessage}")
        StationConfig(Callsign(""), "", "")

/*
  def pane(): Pane =
    logger.debug(s"Creating station form for $station")
    val myCaseForm = MyCaseForm[Station](station)
    logger.trace("New station result will be handled by container")
    myCaseForm.pane()*/
