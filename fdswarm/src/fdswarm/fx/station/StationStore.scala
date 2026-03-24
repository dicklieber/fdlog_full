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

package fdswarm.fx.station

import fdswarm.io.DirectoryProvider
import fdswarm.model.StationConfig
import jakarta.inject.{Inject, Singleton}
import _root_.io.circe.Printer
import _root_.io.circe.parser.decode
import _root_.io.circe.syntax.*
import scalafx.beans.property.ObjectProperty

@Singleton
final class StationStore @Inject() (directoryProvider: DirectoryProvider) {

  private val stationFile: os.Path =
    directoryProvider() / "station.json"

  /** Observable current station. Listen to changes via station.onChange { ... } */
  val station: ObjectProperty[StationConfig] =
    ObjectProperty[StationConfig](this, "station", loadOrDefault())

  /** Persist current station value to station.json */
  def save(): Unit =
    saveToDisk(station.value)

  /** Replace station (fires change listeners) + persist */
  def update(newStation: StationConfig): Unit = {
    station.value = newStation
    saveToDisk(newStation)
  }

  // ---------- internals ----------

  private def loadOrDefault(): StationConfig =
    if os.exists(stationFile) then
      parseStation(os.read(stationFile)) match
        case Right(s) => s
        case Left(_)  => StationConfig()
    else
      StationConfig()

  private val printer: Printer = Printer.spaces2.copy(dropNullValues = true)

  private def saveToDisk(s: StationConfig): Unit = {
    os.makeDir.all(stationFile / os.up)
    os.write.over(stationFile, printer.print(s.asJson))
  }

  private def parseStation(json: String): Either[String, StationConfig] =
    decode[StationConfig](json).left.map(_.getMessage)
}