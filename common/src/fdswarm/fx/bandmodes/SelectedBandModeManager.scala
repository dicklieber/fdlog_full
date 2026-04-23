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

package fdswarm.fx.bandmodes

import fdswarm.logging.LazyStructuredLogging
import fdswarm.StartupInfo
import fdswarm.fx.bands.BandModeBuilder
import fdswarm.model.BandMode
import io.circe.parser.*
import io.circe.syntax.*
import jakarta.inject.{Inject, Singleton}
import scalafx.beans.property.ObjectProperty

@Singleton
final class SelectedBandModeManager @Inject()(dirProvider: fdswarm.DirectoryProvider,
                                              bandModeBuilder: BandModeBuilder,
                                              startupInfo: StartupInfo) extends LazyStructuredLogging:

  private val dir: os.Path =
    val p = dirProvider()
    os.makeDir.all(p)
    p

  private val path: os.Path =
    dir / "selected-bandmode.json"

  val selected: ObjectProperty[BandMode] = ObjectProperty(load())

  selected.onChange { (_, _, newValue) =>
    persist(newValue)
  }

  def save(value: BandMode): Unit =
    selected.value = value

  private def load(): BandMode =
    if startupInfo.info.isDefined then
      startupInfo.info.get.bandMode
    else
      try
        decode[BandMode](os.read(path)).toTry.get
      catch case e: Throwable =>
        val bandMode = bandModeBuilder("20m", "PH")
        logger.warn(s"Could not load bandmode from $path: ${e.getMessage}, using $bandMode!")
        bandMode

  private def persist(value: BandMode): Unit =
    val json = value.asJson.spaces2
    val tmp  = path / os.up / s".${path.last}.tmp"
    os.write.over(tmp, json, createFolders = true)
    os.move.over(tmp, path)