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
import fdswarm.replication.NodeAddress
import fdswarm.util.Ids

import java.time.Instant
import java.util.UUID
import fdswarm.util.Ids.Id
import upickle.ReadWriter
import fdswarm.util.JavaTimePickle.given_ReadWriter_Instant
import upickle.implicits.serializeDefaults
/**
 * This is what's in the store and journal.log.
 *
 * @param callSign    of the worked station.
 * @param contestClass    from the worked station.
 * @param bandMode    that was used.
 * @param stamp       when QSO occurred.
 * @param uuid        id unique QSO id in time & space.
 * @param qsoMetadata info about ur station.
 */
case class Qso(callSign: Callsign,
               contestClass: String,
               section:String,
               bandMode: BandMode,
               qsoMetadata: QsoMetadata,
               @serializeDefaults(true)
               stamp: Instant = Instant.now(),
               uuid: Id = Ids.generateId()) extends  LazyLogging derives ReadWriter:
  lazy val display: String = s"$callSign on $bandMode in $fdHour"
  lazy val fdHour: FdHour =
    FdHour(stamp)

  def isDup(that: Qso): Boolean =
    this.callSign == that.callSign &&
      this.bandMode == that.bandMode
/*
*
 * @see https://wwrof.org/cabrillo/cabrillo-qso-data/
 * @return frequency for a cab file.
 *         As integer KHz
 
def cabFreq: String = {

  mHz.map { mHz =>
    val kHz = mHz * 1000.0F
    f"${kHz}%.0f"
  }.getOrElse(bandMode.cabFreq)
}*/


/*
  def log(): Unit = 
  
    logger.info("newqso {} {} {} {} {} {}",
      kv("callsign", callSign),
      kv("operator", qsoMetadata.operator),
      kv("class", exchange.entryClass),
      kv("section", exchange.sectionCode),
      kv("band", bandMode.bandName),
      kv("mode", bandMode.modeName),
      kv("frequency", mHz),
    )
*/


object Qso:

  given Ordering[Qso] =
    Ordering.by(_.stamp)

  def apply(callSign: Callsign,
            exchange: Exchange,
            bandMode: BandMode
           )(using qsoMetadata: QsoMetadata): Qso =

    Qso(callSign = callSign,
      contestClass = exchange.fdClass.toString,
      section = exchange.sectionCode,
      bandMode = bandMode,
      qsoMetadata = qsoMetadata)


/**
 * This is what gets multi-casted to cluster.
 *
 * @param qso         the new QSO
 * @param nodeAddress where this came from.
 */
case class DistributedQso(qso: Qso, nodeAddress: NodeAddress) extends  LazyLogging





