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

package fdswarm.model

import fdswarm.fx.bands.HamBand
import fdswarm.fx.caseForm.ChoiceField
import io.circe.Codec
import upickle.default.*

/** Persistable representation of Station.
 *
 * IMPORTANT:
 *   Station (UI model) currently contains ChoiceField[HamBand] which is not directly serializable.
 *   This DTO strips UI wrappers and stores the selected HamBand value.
 */
final case class StationPersisted(
                                   bandName: String,
                                   mode:     BandMode.Mode,
                                   rig:      String,
                                   antenna:  String,
                                   operator: Callsign
                                 ) derives  Codec.AsObject

object StationPersisted:

  /** Extract the selected HamBand from a ChoiceField.
   *
   * Based on your compilation error, `cf.value` is already a HamBand.
   * If your ChoiceField changes shape later, this is the only place to update.
   */
  private def selectedHamBand(cf: ChoiceField[HamBand]): HamBand =
    cf.value
