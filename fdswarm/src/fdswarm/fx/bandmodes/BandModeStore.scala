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

import fdswarm.io.FileHelper
import io.circe.Codec
import jakarta.inject.{Inject, Singleton}
import scalafx.beans.property.ObjectProperty

@Singleton
final class BandModeStore @Inject() (fileHelper:FileHelper) {
  import BandModeStore.BandModes

  private val file = "bandmodes.json"

  private def load(): BandModes =
    fileHelper.loadOrDefault(file)(BandModes(Set.empty, Set.empty, Map.empty))

  private def save(bandModes: BandModes): Unit =
    fileHelper.save(file, bandModes)


  private val state: ObjectProperty[BandModes] =
    ObjectProperty(load())


  /** Exposes the persisted state as an observable property so UIs can react immediately. */
  val bandModes: ObjectProperty[BandModes] = state

  state.onChange { (_, _, nv) =>
    save(nv)
  }

  def currentBandMode: BandModes =
    state.value

  def setBands(bands: Set[String]): Unit =
    state.value = state.value.copy(bands = bands)

  def setModes(modes: Set[String]): Unit =
    state.value = state.value.copy(modes = modes)

  def setEnabled(enabled: Map[String, Set[String]]): Unit =
    state.value = state.value.copy(enabled = enabled)

  /** Update only enabled matrix; keep selected bands/modes. */
  def updateEnabledOnly(enabled: Map[String, Set[String]]): Unit =
    state.value = state.value.copy(enabled = enabled)

  // ---- helpers for UI wiring ----

  def isEnabled(mode: String, band: String): Boolean =
    state.value.enabled.getOrElse(mode, Set.empty).contains(band)

  def bandsForMode(mode: String): Set[String] =
    state.value.enabled.getOrElse(mode, Set.empty)

  def modesForBand(band: String): Set[String] =
    state.value.enabled.collect { case (m, bs) if bs.contains(band) => m }.toSet
}

object BandModeStore {
  import sttp.tapir.Schema
  final case class BandModes(
                              bands:   Set[String],
                              modes:   Set[String],
                              enabled: Map[String, Set[String]]
                            ) derives Codec.AsObject, Schema
}
