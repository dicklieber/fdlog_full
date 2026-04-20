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

import fdswarm.fx.contest.ContestConfig
import fdswarm.logging.LazyStructuredLogging
import fdswarm.logging.Locus.Replication
import fdswarm.model.Qso
import io.circe.generic.auto.deriveDecoder
import io.circe.parser.decode
import jakarta.inject.{Inject, Singleton}
import nl.grons.metrics4.scala.DefaultInstrumented

import scala.collection.mutable.ListBuffer

/** Handles node status updates, status broadcasts, QSO processing, and contest restarts. This class is responsible for
  * managing the processing of incoming UDP packets related to various services, such as node status, QSO, and contest
  * configurations. It facilitates the updating of swarm data, processing of status messages, and communication with
  * other components for replication and status broadcast.
  *
  * Dependencies are injected to enable functionality for swarm data updates, status processing, replication support,
  * contest configuration management, and metrics instrumentation.
  *
  * Thread-based processing ensures continuous handling of incoming requests without blocking other operations.
  *
  * Constructor parameters:
  *   - `transport`: Provides incoming replication messages and queue access.
  */
@Singleton
class NodeStatusDispatcher @Inject() (transport: Transport)
    extends LazyStructuredLogging(Replication) with DefaultInstrumented:

  logger.debug("Starting NodeStatusHandler")
  private val nodeStatusListeners = ListBuffer.empty[NodeStatus => Unit]
  private val thread = new Thread(
    () =>
      while !Thread.currentThread().isInterrupted do
        try
          val udpHeader: UDPHeaderData = transport.incomingQueue.take()
          logger.trace(s"Received UDP packet from ${udpHeader.nodeIdentity} for service ${udpHeader.service}")
          receivedPacketCount.inc(1)
          udpHeader.service match
            case Service.Status =>
              statusPackets.inc(1)
              val statusMessage: StatusMessage = udpHeader.decode[StatusMessage]
              lastStatusMessagePayloadSize = udpHeader.payload.length.toDouble

              val nodeStatus = NodeStatus(statusMessage, udpHeader.nodeIdentity, isLocal = false)
              notifyNodeStatusListeners(nodeStatus)
              drainQueuedMessagesAfterStatus()
            case Service.QSO =>
              receivedQsoCount.inc(1)
              val qso: Qso = udpHeader.decode[Qso]
              qsoListener.foreach(_(qso))

            case Service.SendStatus =>
              logger.info(s"Received SendStatus from ${udpHeader.nodeIdentity}")
              sendStatusCount.inc(1)
              val listener = this.synchronized { sentStatusListener }
              listener match
                case Some(callback) =>
                  try callback()
                  catch case e: Exception => logger.error("Error in send status listener", e)
                case None => logger.warn("Dropping SendStatus because no send status listener is registered")

            case Service.RestartContest =>
              logger.info(s"Received RestartContest from ${udpHeader.nodeIdentity}")
              val newConfig: ContestConfig = udpHeader.decode[ContestConfig]
              notifyContestRestartListener(newConfig)
        catch
          case _: InterruptedException => Thread.currentThread().interrupt()

          case e: Exception =>
            e.printStackTrace()
            logger.error(s"Error in Repl processing loop ${e.getMessage}", e)
    ,
    "Repl-Processor"
  )
  private val receivedPacketCount = metrics.counter("received_packets")
  private val statusMessageCount = metrics.counter("status_count")
  private val statusPackets = metrics.counter("status_packets")
  private val sendStatusCount = metrics.counter("send_status_count")
  private val receivedQsoCount = metrics.counter("received_qso_count")
  private var qsoListener: Option[Qso => Unit] = None
  private var sentStatusListener: Option[() => Unit] = None
  private var contestRestartListener: Option[ContestConfig => Unit] = None

  thread.setDaemon(true)
  logger.debug("Starting NodeStatusHandler Thread")
  thread.start()
  private var lastStatusMessagePayloadSize: Double = 0.0

  def addNodeStatusListener(listener: NodeStatus => Unit): () => Unit =
    this.synchronized { nodeStatusListeners += listener }
    () => this.synchronized { nodeStatusListeners -= listener }

  def addQsoListener(listener: Qso => Unit): Unit =
    require(qsoListener.isEmpty, "QSO listener already set")
    qsoListener = Some(listener)

  def addSentStatusListener(listener: () => Unit): Unit =
    require(sentStatusListener.isEmpty, "Send status listener already set")
    sentStatusListener = Some(listener)

  def addContestRestartListener(listener: ContestConfig => Unit): Unit =
    require(contestRestartListener.isEmpty, "Contest restart listener already set")
    contestRestartListener = Some(listener)

  private def notifyNodeStatusListeners(nodeStatus: NodeStatus): Unit = currentNodeStatusListeners.foreach(listener =>
    try listener(nodeStatus)
    catch case e: Exception => logger.error("Error in node status listener", e))

  private def currentNodeStatusListeners: Seq[NodeStatus => Unit] = this.synchronized { nodeStatusListeners.toList }

  private def notifyContestRestartListener(newConfig: ContestConfig): Unit =
    val listener = this.synchronized { contestRestartListener }
    listener match
      case Some(callback) =>
        try callback(newConfig)
        catch case e: Exception => logger.error("Error in contest restart listener", e)
      case None => logger.warn("Dropping RestartContest because no restart listener is registered")

  private def drainQueuedMessagesAfterStatus(): Unit =
    var queuedMessage = transport.incomingQueue.poll()
    while queuedMessage != null do
      logger.debug(s"Drained queued message from ${queuedMessage.nodeIdentity} for service ${queuedMessage.service}")
      if queuedMessage.service == Service.QSO then
//        logger.debug("Processing drained QSO from {}", queuedMessage.nodeIdentity)
      {
        receivedQsoCount.inc(1)
        val sJson = new String(queuedMessage.payload, "UTF-8")
        decode[Qso](sJson) match
          case Right(qso) =>
            logger.debug(s"Received QSO via multicast: ${qso.callsign}")
            {
              val listener = this.synchronized { qsoListener }
              listener match
                case Some(callback) =>
                  try callback(qso)
                  catch case e: Exception => logger.error("Error in QSO listener", e)
                case None => logger.warn("Dropping received QSO because no QSO listener is registered")
            }
          case Left(error) => logger.error(s"Failed to decode QSO from multicast: $sJson", error)
      }
      queuedMessage = transport.incomingQueue.poll()
