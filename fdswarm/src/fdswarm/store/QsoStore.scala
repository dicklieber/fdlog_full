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

import fdswarm.StartupInfo
import fdswarm.io.DirectoryProvider
import fdswarm.logging.LazyStructuredLogging
import fdswarm.logging.Locus.LogEntry
import fdswarm.model.*
import fdswarm.fx.qso.FdHour
import fdswarm.replication.HashCount
import fdswarm.replication.status.SwarmData
import fdswarm.replication.{Service, Transport}
import fdswarm.util.OtelMetrics
import fdswarm.util.Ids.Id
import io.circe.generic.auto.deriveDecoder
import io.circe.parser.decode
import jakarta.inject.*
import javafx.application.Platform
import scalafx.collections.ObservableBuffer

import scala.collection.concurrent.TrieMap

@Singleton
class QsoStore @Inject() (
    directoryProvider: DirectoryProvider,
    otelMetrics: OtelMetrics,
    transport: Transport,
    swarmDataProvider: Provider[SwarmData],
    startupInfo: StartupInfo,
    filenameStamp: fdswarm.util.FilenameStamp)
    extends LazyStructuredLogging(LogEntry):

  val qsoCollection: ObservableBuffer[Qso] = new ObservableBuffer[Qso]()
  protected val map: TrieMap[Id, Qso] = new TrieMap
  protected val internalDigests: TrieMap[FdHour, FdHourDigest] = new TrieMap
  private val journalFile = directoryProvider() / "qsosJournal.json"
  var idsHash: String = ""

  def add(
      qso: Qso
    ): StyledMessage =
    val isDuplicateInStore =
      map.values.exists(existing => existing.dupCriterion == qso.dupCriterion)
    if isDuplicateInStore then StyledMessage(qso.rejectedMsg, "duplicate-qso")
    else
      val jsonString = qso.asJsonCompact
      os.write.append(journalFile, jsonString + "\n", createFolders = true)
      logger.info("qso" -> qso)
      addToMap(qso)
      mutateQsoCollection {
        qsoCollection.prepend(qso)
      }
      calculateHash()
      val bytes: Array[Byte] = jsonString.getBytes("UTF-8")
      transport.send(Service.QSO, bytes)
      StyledMessage(s"Added ${qso.dupCriterion} to store", "addQsoOk")

  private def addToMap(
      qso: Qso
    ): Unit =
    val uuid = qso.uuid
    val maybeQso = map.putIfAbsent(uuid, qso)
    maybeQso.foreach(was =>
      logger.error(s"Was already a qso for uuid: $uuid $qso")
    )

  private def calculateHash(): Unit =
    otelMetrics.incrementCounter(
      name = "fdlog.build.hour.digests.count",
      description = "Number of times FD hour digests were built"
    )
    val startNanos = System.nanoTime()
    idsHash =
      try
        fdswarm.replication.calcShaHash(
          map.keys
        )
      finally
        otelMetrics.recordTimerNanos(
          name = "fdlog.build.hour.digests",
          nanos = System.nanoTime() - startNanos,
          description = "Time taken to build hash of all QSOs in the store"
        )
    val nextDigests =
      all
        .groupBy(
          _.fdHour
        )
        .toSeq
        .map(
          (fdHour, qsos) => FdHourDigest(fdHour, qsos)
        )
    internalDigests.clear()
    internalDigests.addAll(
      nextDigests.map(
        digest => digest.fdHour -> digest
      )
    )
    swarmData.updateLocalDigests(
      hashCount = HashCount(
        hash = idsHash,
        qsoCount = map.size
      ),
      digests = digests()
    )

  def add(
      batch: Seq[Qso]
    ): Unit =
    doAdd(batch)

  private def doAdd(
      batch: Seq[Qso]
    ): Unit =
    val thread = Thread.currentThread().getName
    logger.debug(s"[THREAD:$thread] Adding ${batch.size} QSOs to store")

    val toAdd = batch.filter { qso =>
      val uuid = qso.uuid
      val isNew = map.putIfAbsent(uuid, qso).isEmpty
      if !isNew then logger.error(s"Was already a qso for uuid: $uuid $qso")
      isNew
    }

    if toAdd.nonEmpty then
      val lines = toAdd.map(_.asJsonCompact + "\n").mkString
      os.write.append(journalFile, lines, createFolders = true)
      mutateQsoCollection {
        qsoCollection.prependAll(toAdd)
      }
      calculateHash()

  def get(
      uuid: Id
    ): Option[Qso] =
    map.get(uuid)
  def hasQsos: Boolean = map.nonEmpty
  def potentialDups(
      startOfCallsign: String,
      bandmode: BandMode
    ): DupInfo =
    val allPotentialDups: Seq[Qso] = map
      .filter { case (id, qso) =>
        val bandModeMatch = qso.bandMode == bandmode
        val startMatch = qso.callsign.startsWith(startOfCallsign)
        startMatch && bandModeMatch
      }
      .values
      .toSeq
    val frustNDups = allPotentialDups.take(70).map(_.callsign)
    DupInfo(frustNDups, allPotentialDups.size)

  if startupInfo.info.exists(_.clearQsos) then
    logger.info("StartupInfo Clearing QSOs journal")
    os.remove(journalFile)

  if os.exists(journalFile) then
    os.read
      .lines(journalFile)
      .iterator
      .map(_.trim)
      .filter(_.nonEmpty)
      .foreach { line =>
        decode[Qso](line) match
          case Right(qso) =>
            if map.putIfAbsent(qso.uuid, qso).isEmpty then
              mutateQsoCollection {
                qsoCollection.prepend(qso)
              }
          case Left(error) =>
            logger.error(s"Failed to decode Qso from line: $line", error)
      }
    calculateHash()

  def archiveAndClear(): Unit =
    val timestampedFile =
      directoryProvider() / s"${filenameStamp.build()}.qsosJournal.json"
    if os.exists(journalFile) then
      os.move(journalFile, timestampedFile)
      logger.info(s"Archived QSOs to $timestampedFile")

    map.clear()
    internalDigests.clear()
    idsHash = ""
    swarmData.updateLocalDigests(
      hashCount = HashCount(),
      digests = Nil
    )
    mutateQsoCollection {
      qsoCollection.clear()
    }

  def size: Int = map.size

  private def swarmData: SwarmData = swarmDataProvider.get()

  private def mutateQsoCollection(
      mutation: => Unit
    ): Unit =
    if Platform.isFxApplicationThread then mutation
    else Platform.runLater(() => mutation)

  /** Thread-safe snapshot of all QSOs, sorted by stamp. Prefer this over
    * reading `qsoCollection` from non-JavaFX threads (e.g., Cask routes).
    */
  def all: Seq[Qso] =
    map.values.toSeq.sorted

  def digests(): Seq[FdHourDigest] =
    internalDigests.values.toSeq.sorted
