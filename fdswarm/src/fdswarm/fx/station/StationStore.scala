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

import fdswarm.StationConfigManager
import fdswarm.model.StationConfig
import jakarta.inject.{Inject, Singleton}
import scalafx.beans.property.ObjectProperty

@Singleton
final class StationStore @Inject() (stationConfigManager: StationConfigManager) {

  /** Observable current station. Listen to changes via station.onChange { ... } */
  val station: ObjectProperty[StationConfig] =
    stationConfigManager.stationProperty

  /** Persist current station value to station.json */
  def save(): Unit =
    stationConfigManager.setStation(station.value)

  /** Replace station (fires change listeners) + persist */
  def update(newStation: StationConfig): Unit = {
    stationConfigManager.setStation(newStation)
  }
}
