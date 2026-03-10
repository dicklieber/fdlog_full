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

package fdswarm.replication.status

import com.typesafe.scalalogging.LazyLogging
import fdswarm.fx.qso.FdHour
import fdswarm.io.DirectoryProvider
import fdswarm.replication.*
import fdswarm.store.FdHourDigest
import fdswarm.util.{JavaTimeCirce, NodeIdentity, NodeIdentityManager}
import io.circe.*
import io.circe.parser.decode
import io.circe.syntax.*
import jakarta.inject.*
import scalafx.application.Platform
import scalafx.beans.property.ObjectProperty

import java.time.Instant
import scala.collection.concurrent.TrieMap

@Singleton
class SwarmStatus @Inject() (
    directoryProvider: DirectoryProvider,
    nodeIdentityManager: NodeIdentityManager,
    swarmStatusPane: SwarmStatusPane = null
                            ) extends SwarmStatusApi with LazyLogging:
  val nodeMap: TrieMap[NodeIdentity, NodeDetails] = new TrieMap[NodeIdentity, NodeDetails]
  private val statusFile = directoryProvider() / "swarmStatus.json"

  def put(nodeStuff: NodeStuff): Unit =
    logger.whenDebugEnabled {
      val status: StatusMessage = nodeStuff.status
      logger.debug(
        s"putting ${nodeStuff.nodeIdentity} ${nodeStuff.status.fdDigests.size} fdDigests"
      )
    }
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

    for 
      fdHourDigest <- nodeStuff.status.fdDigests
    do
      logger.trace("fdHourDigest: {}", fdHourDigest)
      nodeDetails.put(fdHourDigest, () => ())
    if swarmStatusPane != null then
      swarmStatusPane.update(nodeMap.values.toSeq)
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
              if cellDTO.lhData.lastSeen.isAfter(nodeDetails.lastUpdate.value) then
                nodeDetails.lastUpdate.value = cellDTO.lhData.lastSeen
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
  def clear(): Unit =
    nodeMap.clear()
    save()
    if swarmStatusPane != null then
      swarmStatusPane.update(nodeMap.values.toSeq)
    logger.debug("Cleared all swarm status data.")

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

trait SwarmStatusApi:
  def clear(): Unit