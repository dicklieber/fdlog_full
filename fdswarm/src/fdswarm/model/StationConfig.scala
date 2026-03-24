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
 */

package fdswarm.model

import fdswarm.fx.bands.HamBand
import fdswarm.fx.caseForm.ChoiceField
import fdswarm.model.BandMode.*
import io.circe.Codec
import sttp.tapir.Schema

/**
 * Details about this station.
 *
 */
final case class StationConfig(operator: Callsign = Callsign(""), rig: String = "", antenna: String = "") derives  Codec.AsObject, Schema
//
//object Station:
//
//  import Callsign.given_Conversion_String_Callsign
//  
//  

