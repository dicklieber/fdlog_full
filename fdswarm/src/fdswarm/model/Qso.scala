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

import com.typesafe.scalalogging.LazyLogging
import fdswarm.fx.qso.FdHour
import fdswarm.util.Ids
import fdswarm.util.Ids.Id
import io.circe.Codec

import java.time.Instant
/**
 * This is what's in the store and journal.log.
 *
 * @param callsign    of the worked station.
 * @param contestClass    from the worked station.
 * @param bandMode    that was used.
 * @param stamp       when QSO occurred.
 * @param uuid        id unique QSO id in time & space.
 * @param qsoMetadata info about ur station.
 */
case class Qso(callsign: Callsign,
               contestClass: String,
               section:String,
               bandMode: BandMode,
               qsoMetadata: QsoMetadata,
               stamp: Instant = Instant.now(),
               uuid: Id = Ids.generateId()) extends  LazyLogging derives  Codec.AsObject, sttp.tapir.Schema:
  lazy val display: String = s"$callsign on $bandMode in $fdHour"
  lazy val fdHour: FdHour =
    FdHour(stamp)


object Qso:
  given Ordering[Qso] =
    Ordering.by(_.stamp)

  def apply(callSign: Callsign,
            exchange: Exchange,
            bandMode: BandMode
           )(using qsoMetadata: QsoMetadata): Qso =

    Qso(callsign = callSign,
      contestClass = exchange.fdClass.toString,
      section = exchange.sectionCode,
      bandMode = bandMode,
      qsoMetadata = qsoMetadata)





