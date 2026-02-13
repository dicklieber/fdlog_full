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

import fdswarm.model.BandMode
import jakarta.inject.{Inject, Singleton}

/** Simple validation helper for user-chosen band/mode pairs. */
@Singleton
final class BandModeValidator @Inject() (store: BandModeStore):

  /** Returns None if OK, else an error message. */
  def validate(bm: BandMode): Option[String] =
    val state = store.currentBandMode

    if !state.modes.contains(bm.mode) then
      Some(s"Mode '${bm.mode}' is not selected")
    else if !state.bands.contains(bm.band) then
      Some(s"Band '${bm.band}' is not selected")
    else if !store.isEnabled(bm.mode, bm.band) then
      Some(s"Band/mode '${bm.band}' / '${bm.mode}' is not enabled")
    else
      None