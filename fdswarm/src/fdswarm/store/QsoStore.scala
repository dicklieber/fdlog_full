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
import fdswarm.fx.qso.FdHour
import fdswarm.io.DirectoryProvider
import fdswarm.model.*
import fdswarm.replication.StatusMessage
import fdswarm.util.{HostAndPort, Ids}
import fdswarm.util.Ids.Id
import io.micrometer.core.instrument.{MeterRegistry, Timer}
import jakarta.inject.*
import scalafx.collections.ObservableBuffer
import upickle.default.*

import scala.collection.concurrent.TrieMap
import scala.util.Using

@Singleton
class QsoStore @Inject()(directoryProvider: DirectoryProvider, registry: MeterRegistry) extends LazyLogging:
  val qsoCollection: ObservableBuffer[Qso] = new ObservableBuffer[Qso]()
  private val journalFile = directoryProvider() / "qsosJournal.json"
  private val map: TrieMap[Id, Qso] = new TrieMap
  var fdHourDigests: Map[FdHour, FdHourDigest] = Map.empty

  private val buildDigestsTimer = Timer.builder("fdlog.build.hour.digests")
    .description("Time taken to build FD hour digests")
    .register(registry)

  def add(qso: Qso): Unit =
    val uuid = qso.uuid
    val maybeQso = map.putIfAbsent(uuid, qso)
    os.write.append(journalFile, write(qso) + "\n", createFolders = true)
    qsoCollection.prepend(qso)
    buildFdHourDigests()
    maybeQso.foreach(was =>
      logger.error(s"Was already a qso for uuid: $uuid $qso")
    )

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
    buildFdHourDigests()

  private def buildFdHourDigests(): Unit =
    buildDigestsTimer.record(new Runnable {
      override def run(): Unit = {
        val hourToQsos: Map[FdHour, Seq[Qso]] = all.groupBy(_.fdHour)
        fdHourDigests = hourToQsos.map { case (fdHour, qsos) =>
          fdHour -> FdHourDigest(fdHour, qsos)
        }
      }
    })

  /**
   * Thread-safe snapshot of all QSOs, sorted by stamp.
   * Prefer this over reading `qsoCollection` from non-JavaFX threads (e.g., Cask routes).
   */
  def all: Seq[Qso] =
    map.values.toSeq.sorted

  def potentialDups(startOfCallsign: String, bandmode: BandMode): Seq[Qso] =
    map.filter { case (id, qso) =>
        val bandModeMatch = qso.bandMode == bandmode
        logger.trace("qso.bandMode {} against {} mmatch: {}", qso.bandMode, bandmode, bandModeMatch)
        val startMatch = qso.callSign.startsWith(startOfCallsign)
        startMatch && bandModeMatch
      }
      .values
      .toSeq

  /**
   * Returns the QSOs that are needed for the given hour.
   *
   * @param statusMessage as received from an FdSwarm node.
   * @return all the [[FdHour]]s that aren't on the [[QsoStore]] or don't match.
   */
  def neededQsos(incoming: Seq[FdHourDigest]): Seq[FdHour] =
    // this might be updated by another thread so we save a reference so it can't change under us.
    val cpy: Map[FdHour, FdHourDigest] = fdHourDigests
    incoming.flatMap { remoteFdHourDigest =>
      val remoteFdHour = remoteFdHourDigest.fdHour
      cpy.get(remoteFdHour) match
        // we have one, is it the same?
        case Some(localFdDigest) =>
          Option.when(localFdDigest != remoteFdHourDigest) {
            remoteFdHour
          }
        case None => // we don't have it yet, so we need it.
          Some(remoteFdHour)

    }

  /**
   * current digests for all FdHours.
   *
   * @return
   */
  def digests(): Seq[FdHourDigest] = fdHourDigests.values.toSeq.sorted

  def idsForHour(fdHour: FdHour): FdHourIds =
    val ids = all.filter(_.fdHour == fdHour).map(_.uuid)
    FdHourIds(fdHour, ids)