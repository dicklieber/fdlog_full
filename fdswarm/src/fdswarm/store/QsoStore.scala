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
import fdswarm.StartupInfo
import fdswarm.fx.qso.FdHour
import fdswarm.io.DirectoryProvider
import fdswarm.model.*
import fdswarm.replication.status.SwarmData
import fdswarm.replication.{Service, Transport}
import fdswarm.util.Ids.Id
import io.micrometer.core.instrument.{Counter, MeterRegistry, Timer}
import jakarta.inject.*
import jakarta.inject.Provider
import io.circe.parser.decode
import io.circe.syntax.*
import javafx.application.Platform
import scalafx.collections.ObservableBuffer

import scala.collection.concurrent.TrieMap


@Singleton
class QsoStore @Inject()(directoryProvider: DirectoryProvider,
                         registry: MeterRegistry,
                         transport: Transport,
                         swarmDataProvider: Provider[SwarmData],
                         startupInfo: StartupInfo,
                         filenameStamp: fdswarm.util.FilenameStamp) extends LazyLogging:

  private def swarmData: SwarmData = swarmDataProvider.get()
  val qsoCollection: ObservableBuffer[Qso] = new ObservableBuffer[Qso]()
  protected val map: TrieMap[Id, Qso] = new TrieMap
  private val journalFile = directoryProvider() / "qsosJournal.json"
  private val buildDigestsTimer = Timer.builder("fdlog.build.hour.digests")
    .description("Time taken to build FD hour digests")
    .register(registry)
  private val buildDigestsCounter = Counter.builder("fdlog.build.hour.digests.count")
    .description("Number of times FD hour digests were built")
    .register(registry)
  private var fdHourDigests: Map[FdHour, FdHourDigest] = Map.empty

  private def mutateQsoCollection(mutation: => Unit): Unit =
    if Platform.isFxApplicationThread then
      mutation
    else
      Platform.runLater(() => mutation)

  def add(qso: Qso): StyledMessage =
    val isDuplicateInStore = map.values.exists(existing =>
      existing.dupCriterion == qso.dupCriterion
    )
    if isDuplicateInStore then
      StyledMessage(qso.rejectedMsg, "duplicate-qso")
    else
      val jsonString = qso.asJsonCompact
      os.write.append(journalFile, jsonString + "\n", createFolders = true)

      addToMap(qso)
      mutateQsoCollection {
        qsoCollection.prepend(qso)
      }
      buildFdHourDigests()
      val bytes: Array[Byte] = jsonString.getBytes("UTF-8")
      transport.send(Service.QSO, bytes)
      StyledMessage(s"Added ${qso.dupCriterion} to store", "addQsoOk")

  private def addToMap(qso: Qso): Unit =
    val uuid = qso.uuid
    val maybeQso = map.putIfAbsent(uuid, qso)
    maybeQso.foreach(was =>
      logger.error(s"Was already a qso for uuid: $uuid $qso")
    )

  def add(batch: Seq[Qso]): Unit =
    doAdd(batch)


  def get(uuid: Id): Option[Qso] =
    map.get(uuid)
  def hasQsos: Boolean = map.nonEmpty
  def potentialDups(startOfCallsign: String, bandmode: BandMode): DupInfo =
    val allPotentialDups: Seq[Qso] = map.filter { case (id, qso) =>
        val bandModeMatch = qso.bandMode == bandmode
        logger.trace("qso.bandMode {} against {} mmatch: {}", qso.bandMode, bandmode, bandModeMatch)
        val startMatch = qso.callsign.startsWith(startOfCallsign)
        startMatch && bandModeMatch
      }
      .values
      .toSeq
    val frustNDups = allPotentialDups.take(70).map(_.callsign)
    DupInfo(frustNDups, allPotentialDups.size)

  /**
   * current digests for all FdHours.
   *
   * @return
   */
  def digests(): Seq[FdHourDigest] = fdHourDigests.values.toSeq.sorted

  if startupInfo.info.exists(_.clearQsos) then
    logger.info("StartupInfo Clearing QSOs journal")
    os.remove(journalFile)
  
  if os.exists(journalFile) then
    os.read.lines(journalFile)
      .iterator
      .map(_.trim)
      .filter(_.nonEmpty)
      .foreach { line =>
        decode[Qso](line) match
          case Right(qso) =>
            if (map.putIfAbsent(qso.uuid, qso).isEmpty) {
              mutateQsoCollection {
                qsoCollection.prepend(qso)
              }
            }
          case Left(error) =>
            logger.error(s"Failed to decode Qso from line: $line", error)
      }
    buildFdHourDigests()

  def archiveAndClear(): Unit =
    val timestampedFile = directoryProvider() / s"${filenameStamp.build()}.qsosJournal.json"
    if os.exists(journalFile) then
      os.move(journalFile, timestampedFile)
      logger.info(s"Archived QSOs to $timestampedFile")
    
    map.clear()
    fdHourDigests = Map.empty
    swarmData.updateLocalDigests(Nil)
    mutateQsoCollection {
      qsoCollection.clear()
    }

  private def doAdd(batch: Seq[Qso]): Unit =
    val thread = Thread.currentThread().getName
    logger.debug(s"[THREAD:$thread] Adding ${batch.size} QSOs to store")

    val toAdd = batch.filter { qso =>
      val uuid = qso.uuid
      val isNew = map.putIfAbsent(uuid, qso).isEmpty
      if !isNew then
        logger.error(s"Was already a qso for uuid: $uuid $qso")
      isNew
    }

    if toAdd.nonEmpty then
      val lines = toAdd.map(_.asJsonCompact + "\n").mkString
      os.write.append(journalFile, lines, createFolders = true)
      mutateQsoCollection {
        qsoCollection.prependAll(toAdd)
      }
      buildFdHourDigests()

  private def buildFdHourDigests(): Unit =
    logger.debug("Rebuilding FDhour digests")
    buildDigestsCounter.increment()
    buildDigestsTimer.record(new Runnable {
      override def run(): Unit =
        val hourToQsos: Map[FdHour, Seq[Qso]] = all.groupBy(_.fdHour)
        fdHourDigests = hourToQsos.map { case (fdHour, qsos) =>
          fdHour -> FdHourDigest(fdHour, qsos)
        }
        swarmData.updateLocalDigests(fdHourDigests.values.toSeq)
    })

  /**
   * Thread-safe snapshot of all QSOs, sorted by stamp.
   * Prefer this over reading `qsoCollection` from non-JavaFX threads (e.g., Cask routes).
   */
  def all: Seq[Qso] =
    map.values.toSeq.sorted

  private[store] def internalDigests: Map[FdHour, FdHourDigest] = fdHourDigests
