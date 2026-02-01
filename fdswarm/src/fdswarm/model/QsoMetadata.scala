
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

import com.organization.BuildInfo
import upickle.default.*
import upickle.implicits.*

/**
 * Stuff about a QSO. i.e. not entered as a part of a QSO itself
 *
 * @param station can be edited by user.
 * @param node    what node, in the cluster this came from.
 *                // * @param contestId so old data can't accident be missed with current.
 *
 * @param v       FdSwarm Version that built this so we can detect mismatched versions.
 */

@serializeDefaults(true)

case class QsoMetadata(station: StationPersisted,
                       node: String = "localhost;1",
                       contest: Contest,
                        v: String = BuildInfo.version) derives ReadWriter
//  def forStation(station: Station):QsoMetadata =
//    copy(operator =  station.operator, rig= station.rig, ant = station.antenna)


//   def qso(callSign: CallSign, exchange: Exchange, bandMode: BandMode, stamp:Instant = Instant.now): Qso = {
//     Qso(callSign = callSign.toUpperCase, exchange = exchange, bandMode = bandMode, qsoMetadata = value, stamp = stamp)
//   }
//
//  def qso(callSign: CallSign, exchange: Exchange, station: Station) :Qso = {
//    Qso(callSign = callSign.toUpperCase, exchange = exchange, bandMode = station.bandMode, qsoMetadata = value.forStation(station))
//  }

//
//trait QsoBuilder{
//  def qso(callSign: CallSign, exchange: Exchange, bandMode: BandMode, stamp:Instant = Instant.now): Qso
//}