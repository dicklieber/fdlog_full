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

package fdswarm.replication

import com.typesafe.scalalogging.LazyLogging
import fdswarm.fx.qso.FdHour
import fdswarm.io.DirectoryProvider
import fdswarm.store.FdHourDigest
import fdswarm.util.{JavaTimeCirce, NodeIdentity, NodeIdentityManager}
import io.circe.*
import io.circe.syntax.*
import io.circe.parser.decode
import jakarta.inject.*
import javafx.beans.value.ObservableObjectValue
import scalafx.application.Platform
import scalafx.beans.property.{IntegerProperty, ObjectProperty}
import scalafx.collections.ObservableMap

import java.time.Instant
import scala.collection.concurrent.TrieMap

@Singleton
class SwarmStatus @Inject() (
    directoryProvider: DirectoryProvider,
    nodeIdentityManager: NodeIdentityManager
) extends LazyLogging:
  val nodeMap: ObservableMap[NodeIdentity, NodeDetails] =
    ObservableMap[NodeIdentity, NodeDetails]()
  private val statusFile = directoryProvider() / "swarmStatus.json"

  def put(nodeStuff: NodeStuff): Unit =
    logger.debug(
      s"putting ${nodeStuff.nodeIdentity} ${nodeStuff.status.fdDigests.size} fdDigests"
    )
    val nodeIdentity = nodeStuff.nodeIdentity
    val nodeDetails = nodeMap.getOrElseUpdate(
      nodeIdentity, {
        val details = NodeDetails(nodeIdentity)
        try
          Platform.runLater {
            nodeMap.put(nodeIdentity, details)
          }
        catch
          case _: IllegalStateException =>
            nodeMap.put(nodeIdentity, details)
        details
      }
    )

    for fdHourDigest <- nodeStuff.status.fdDigests
    do
      logger.trace("fdHourDigest: {}", fdHourDigest)
      nodeDetails.put(fdHourDigest, () => ())

    save()

  // Load state on startup
  try
    if os.exists(statusFile) then
      val json = os.read(statusFile)
      decode[Map[NodeIdentity, NodeDetailsDTO]](json) match
        case Right(dtoMap: Map[NodeIdentity, NodeDetailsDTO]) =>
          dtoMap.foreach { (nodeIdentity, dto) =>
            val nodeDetails = NodeDetails(nodeIdentity)
            dto.cells.foreach { cellDTO =>
              val cell = FdHourNodeCell(nodeIdentity, cellDTO.fdHour)
              cell.lhData.value = cellDTO.lhData
              nodeDetails.map.put(cellDTO.fdHour, cell)
            }
            nodeDetails.recalculateQsoCount()
            nodeMap.put(nodeIdentity, nodeDetails)
          }
          logger.info(s"Loaded swarm status from $statusFile")
        case Left(decodeError) =>
          logger.error(
            s"Failed to decode swarm status from $statusFile: $decodeError"
          )
  catch
    case e: Exception =>
      logger.error(s"Error loading swarm status: ${e.getMessage}")

  def updateLocalDigests(digests: Seq[FdHourDigest]): Unit =
    val nodeIdentity = ourNodeIdentity
    val nodeDetails = nodeMap.getOrElseUpdate(
      nodeIdentity, {
        val details = NodeDetails(nodeIdentity)
        try
          Platform.runLater {
            nodeMap.put(nodeIdentity, details)
          }
        catch
          case _: IllegalStateException =>
            nodeMap.put(nodeIdentity, details)
        details
      }
    )

    digests.foreach { fdHourDigest =>
      nodeDetails.put(fdHourDigest, () => ())
    }
    save()

  def ourNodeIdentity: NodeIdentity = nodeIdentityManager.nodeIdentity

  private def save(): Unit =
    try
      val dtoMap = nodeMap.map { (id, details) =>
        val cellDTOs = details.map.values.map { cell =>
          FdHourNodeCellDTO(cell.fdHour, cell.lhData.value)
        }.toSeq
        id -> NodeDetailsDTO(cellDTOs)
      }.toMap
      val json = dtoMap.asJson.noSpaces
      os.write.over(statusFile, json, createFolders = true)
      logger.trace(s"Saved swarm status to $statusFile")
    catch
      case e: Exception =>
        logger.error(s"Error saving swarm status: ${e.getMessage}")

case class LHData(fdHourDigest: FdHourDigest, lastSeen: Instant = Instant.EPOCH)
object LHData:
  import JavaTimeCirce.given
  given Codec[LHData] = Codec.AsObject.derived

case class FdHourNodeCellDTO(fdHour: FdHour, lhData: LHData)
    derives Codec.AsObject
case class NodeDetailsDTO(cells: Seq[FdHourNodeCellDTO]) derives Codec.AsObject

case class FdHourNodeCell(nideIdentity: NodeIdentity, fdHour: FdHour):
  val lhData: ObjectProperty[LHData] =
    ObjectProperty[LHData](LHData(FdHourDigest.empty(fdHour)))

class NodeDetails(nodeIdentity: NodeIdentity):
  val map: TrieMap[FdHour, FdHourNodeCell] = new TrieMap[FdHour, FdHourNodeCell]
  val qsoCount: IntegerProperty = IntegerProperty(0)
  val lastUpdate: ObjectProperty[Instant] = ObjectProperty[Instant](Instant.EPOCH)

  def put(fdHourDigest: FdHourDigest, onUpdate: () => Unit): Unit =
    val cell = map.getOrElseUpdate(
      fdHourDigest.fdHour,
      FdHourNodeCell(nodeIdentity, fdHourDigest.fdHour)
    )
    val data = LHData(fdHourDigest, Instant.now())
    try
      Platform.runLater {
        cell.lhData.value = data
        recalculateQsoCount()
        lastUpdate.value = Instant.now()
        onUpdate()
      }
    catch
      case _: IllegalStateException =>
        // Fallback for tests or headless environments where Toolkit is not initialized
        cell.lhData.value = data
        recalculateQsoCount()
        lastUpdate.value = Instant.now()
        onUpdate()

  def recalculateQsoCount(): Unit =
    qsoCount.value = map.values.map(_.lhData.value.fdHourDigest.count).toSeq.sum
