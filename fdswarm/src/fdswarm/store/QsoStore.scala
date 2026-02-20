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
import fdswarm.util.Ids.Id
import io.micrometer.core.instrument.{Counter, MeterRegistry, Timer}
import jakarta.inject.*
import io.circe.parser.decode
import io.circe.syntax.*
import scalafx.collections.ObservableBuffer

import scala.collection.concurrent.TrieMap

@Singleton
class QsoStore @Inject()(directoryProvider: DirectoryProvider, registry: MeterRegistry) extends LazyLogging:
  val qsoCollection: ObservableBuffer[Qso] = new ObservableBuffer[Qso]()
  private val journalFile = directoryProvider() / "qsosJournal.json"
  protected val map: TrieMap[Id, Qso] = new TrieMap
  private val buildDigestsTimer = Timer.builder("fdlog.build.hour.digests")
    .description("Time taken to build FD hour digests")
    .register(registry)
  private val buildDigestsCounter = Counter.builder("fdlog.build.hour.digests.count")
    .description("Number of times FD hour digests were built")
    .register(registry)
  private var fdHourDigests: Map[FdHour, FdHourDigest] = Map.empty
  private[store] def internalDigests: Map[FdHour, FdHourDigest] = fdHourDigests

  def add(qso: Qso): Unit =
    add(Seq(qso))

  def add(batch: Seq[Qso]): Unit =
    val lines = for
      qso <- batch
    yield
      val uuid = qso.uuid
      val maybeQso = map.putIfAbsent(uuid, qso)
      qsoCollection.prepend(qso)
      maybeQso.foreach(was =>
        logger.error(s"Was already a qso for uuid: $uuid $qso")
      )
      qso.asJson.noSpaces + "\n"

    if lines.nonEmpty then
      os.write.append(journalFile, lines.mkString, createFolders = true)
    buildFdHourDigests()

  private def buildFdHourDigests(): Unit =
    buildDigestsCounter.increment()
    buildDigestsTimer.record(new Runnable {
      override def run(): Unit =
        val hourToQsos: Map[FdHour, Seq[Qso]] = all.groupBy(_.fdHour)
        fdHourDigests = hourToQsos.map { case (fdHour, qsos) =>
          fdHour -> FdHourDigest(fdHour, qsos)
        }
    })

  if os.exists(journalFile) then
    os.read.lines(journalFile)
      .iterator
      .map(_.trim)
      .filter(_.nonEmpty)
      .foreach { line =>
        decode[Qso](line) match
          case Right(qso) =>
            map.put(qso.uuid, qso)
            qsoCollection.prepend(qso)
          case Left(error) =>
            logger.error(s"Failed to decode Qso from line: $line", error)
      }
    buildFdHourDigests()

  def get(uuid: Id): Option[Qso] =
    map.get(uuid)

  def potentialDups(startOfCallsign: String, bandmode: BandMode): Seq[Qso] =
    map.filter { case (id, qso) =>
        val bandModeMatch = qso.bandMode == bandmode
        logger.trace("qso.bandMode {} against {} mmatch: {}", qso.bandMode, bandmode, bandModeMatch)
        val startMatch = qso.callsign.startsWith(startOfCallsign)
        startMatch && bandModeMatch
      }
      .values
      .toSeq


  /**
   * current digests for all FdHours.
   *
   * @return
   */
  def digests(): Seq[FdHourDigest] = fdHourDigests.values.toSeq.sorted

  /**
   * Thread-safe snapshot of all QSOs, sorted by stamp.
   * Prefer this over reading `qsoCollection` from non-JavaFX threads (e.g., Cask routes).
   */
  def all: Seq[Qso] =
    map.values.toSeq.sorted