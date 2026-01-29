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
import fdswarm.fx.caseForm.MyCaseForm
import fdswarm.io.{DirectoryProvider, ProductionDirectory}
import fdswarm.model.Station
import jakarta.inject.{Inject, Singleton}
import scalafx.scene.layout.Pane
import upickle.default.*

@Singleton
class StationManager @Inject()(productionDirectory:DirectoryProvider) extends LazyLogging:
  private var internalStationProvate: Station = Station.defaultStation

  private val file: os.Path = productionDirectory() / "station.json"

  def station: Station =
    internalStationProvate
  private def setStation(station: Station):Unit=
    internalStationProvate = station

  load()

  def pane(): Pane =
    logger.info(s"Creating station form for $station")
    val myCaseForm = MyCaseForm[Station](station, newStation =>
      logger.trace("New station: {}", newStation)
      setStation( newStation)
      save()
    )
    myCaseForm.pane()

  /** Save current _station to station.json. */
  def save(): Unit =
    try
      // Ensure parent dir exists (in case you later change `file` to a nested path)
      os.makeDir.all(file / os.up)
      logger.trace("Saving station to {} station: {}", file, internalStationProvate)
      val json: String = write(internalStationProvate, indent = 2)
      logger.trace("JSON: {}", json)
      os.write.over(file, json)
      logger.info(s"Saved station to $file")
    catch
      case t: Throwable =>
        logger.error(s"Failed to save station to $file", t)

  /** Load station.json (if present) into _station. Keeps default Station() if missing or invalid. */
  def load(): Unit =
    try
      if os.exists(file) then
        val json = os.read(file)
        setStation(read[Station](json))
        logger.info(s"Loaded station from $file")
      else
        logger.info(s"No station file at $file; using defaults")
    catch
      case t: Throwable =>
        logger.warn(s"Failed to load station from $file; using defaults", t)
        setStation(Station.defaultStation)

