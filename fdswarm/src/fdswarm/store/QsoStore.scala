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

package fdswarm.store

import com.typesafe.scalalogging.LazyLogging
import fdswarm.io.DirectoryProvider
import fdswarm.model.*
import fdswarm.replication.FdHourDigest
import fdswarm.util.Ids
import fdswarm.util.Ids.Id
import jakarta.inject.*
import scalafx.collections.ObservableBuffer
import upickle.default.*

import scala.collection.concurrent.TrieMap
import scala.util.Using

@Singleton
class QsoStore @Inject()(directoryProvider: DirectoryProvider) extends LazyLogging:
  val qsoCollection: ObservableBuffer[Qso] = new ObservableBuffer[Qso]()
  private val journalFile = directoryProvider() / "qsosJournal.json"
  private val map: TrieMap[Id, Qso] = new TrieMap
  private var fdHourDigests: Map[FdHour, FdHourDigest] = Map.empty

  def size: Int =
    map.size

  if os.exists(journalFile) then
    os.read.lines(journalFile)
      .iterator
      .map(_.trim)
      .filter(_.nonEmpty)
      .foreach { line =>
        val qso = read[Qso](line)
        map.put(qso.uuid, qso)
        qsoCollection.prepend(qso)
      }

  /**
   * Thread-safe snapshot of all QSOs, sorted by stamp.
   * Prefer this over reading `qsoCollection` from non-JavaFX threads (e.g., Cask routes).
   */
  def all: Seq[Qso] =
    map.values.toSeq.sorted

  def add(qso: Qso): Unit =
    val uuid = qso.uuid
    val maybeQso = map.putIfAbsent(uuid, qso)
    os.write.append(journalFile, write(qso) + "\n", createFolders = true)
    qsoCollection.prepend(qso)
    maybeQso.foreach(was =>
      logger.error(s"Was already a qso for uuid: $uuid $qso")
    )

  def potentialDups(startOfCallsign: String, bandmode: BandMode): Seq[Qso] =
    map.filter{case(id,qso) =>
      val bandModeMatch = qso.bandMode == bandmode
      logger.trace("qso.bandMode {} against {} mmatch: {}", qso.bandMode, bandmode, bandModeMatch)
        val startMatch = qso.callSign.startsWith(startOfCallsign)
        startMatch && bandModeMatch}
      .values
      .toSeq
