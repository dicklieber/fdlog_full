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

import fdswarm.fx.contest.{ContestConfig, ContestConfigManager}
import fdswarm.logging.LazyStructuredLogging
import fdswarm.logging.Locus.Replication
import fdswarm.model.Qso
import fdswarm.replication.status.SwarmData
import fdswarm.store.QsoStore
import io.circe.generic.auto.deriveDecoder
import io.circe.parser.decode
import jakarta.inject.{Inject, Provider, Singleton}
import nl.grons.metrics4.scala.DefaultInstrumented
import scalafx.application.Platform

import java.net.http.HttpClient

/** Handles node status updates, status broadcasts, QSO processing, and contest
  * restarts. This class is responsible for managing the processing of incoming
  * UDP packets related to various services, such as node status, QSO, and
  * contest configurations. It facilitates the updating of swarm data,
  * processing of status messages, and communication with other components for
  * replication and status broadcast.
  *
  * Dependencies are injected to enable functionality for swarm data updates,
  * status processing, replication support, contest configuration management,
  * and metrics instrumentation.
  *
  * Thread-based processing ensures continuous handling of incoming requests
  * without blocking other operations.
  *
  * Constructor parameters:
  *   - `replicationSupportProvider`: Facilitates replication-related tasks,
  *     such as checking and adding QSOs.
  *   - `statusProcessor`: Processes and handles incoming status messages.
  *   - `swarmData`: Maintains information about node statuses in the swarm.
  *   - `transport`: Enables communication through queues for handling various
  *     service-related messages.
  *   - `statusBroadcastService`: Manages the broadcasting of local node
  *     statuses to other nodes.
  *   - `contestManagerProvider`: Provides access to contest configuration
  *     management.
  *   - `meterRegistry`: Instrumentation for registering and tracking metrics
  *     throughout the handler.
  */
@Singleton
class NodeStatusHandler @Inject() (
    qsoStoreProvider: Provider[QsoStore],
    statusProcessor: StatusProcessor,
    swarmData: SwarmData,
    transport: Transport,
    statusBroadcastService: StatusBroadcastService,
    contestManagerProvider: Provider[ContestConfigManager])
    extends LazyStructuredLogging(Replication) with DefaultInstrumented:

  logger.debug("Starting NodeStatusHandler")
  private val httpClient = HttpClient
    .newBuilder()
    .followRedirects(HttpClient.Redirect.NORMAL)
    .build()


  private val thread = new Thread(
    () =>
      while !Thread.currentThread().isInterrupted do
        try
          val udpHeader: UDPHeaderData = transport.incomingQueue.take()
          logger.trace(
            s"Received UDP packet from ${udpHeader.nodeIdentity} for service ${udpHeader.service}"
          )
          receivedPacketCount.inc(1)
          udpHeader.service match
            case Service.Status =>
              statusPackets.inc(1)
              val statusMessage = StatusMessage(udpHeader.payload)
              lastStatusMessagePayloadSize = udpHeader.payload.length.toDouble

              val nodeStatus = NodeStatus(
                statusMessage,
                udpHeader.nodeIdentity,
                isLocal = false
              )
              swarmData.update(nodeStatus)
              //              swarmStatusProviders.put(receivedNodeStatus)
              statusProcessor.processStatus(nodeStatus)
              drainQueuedMessagesAfterStatus()
            case Service.QSO =>
              processQsoMessage(
                udpHeader
              )
            case Service.SendStatus =>
              logger.info(s"Received SendStatus from ${udpHeader.nodeIdentity}")
              sendStatusCount.inc(1)
              statusBroadcastService.broadcastStatus()

            case Service.RestartContest =>
              logger.info(
                s"Received RestartContest from ${udpHeader.nodeIdentity}"
              )
              val sJson = new String(udpHeader.payload, "UTF-8")
              decode[ContestConfig](sJson) match
                case Right(newConfig) =>
                  // ContestConfigManager exposes JavaFX properties; update them on the FX thread.
                  Platform.runLater {
                    contestManager.handleRestartContest(newConfig)
                  }
                case Left(error) =>
                  logger.error(
                    s"Failed to decode ContestConfig from RestartContest: $sJson",
                    error
                  )
        catch
          case _: InterruptedException => Thread.currentThread().interrupt()

          case e: Exception =>
            e.printStackTrace()
            logger.error(s"Error in Repl processing loop ${e.getMessage}", e)
    ,
    "Repl-Processor"
  )
  private var lastStatusMessagePayloadSize: Double = 0.0

  private def contestManager: ContestConfigManager =
    contestManagerProvider.get()

  private def drainQueuedMessagesAfterStatus(): Unit =
    var queuedMessage = transport.incomingQueue.poll()
    while queuedMessage != null do
      logger.debug(
        s"Drained queued message from ${queuedMessage.nodeIdentity} for service ${queuedMessage.service}"
      )
      if queuedMessage.service == Service.QSO then
//        logger.debug("Processing drained QSO from {}", queuedMessage.nodeIdentity)
        processQsoMessage(
          queuedMessage
        )
      queuedMessage = transport.incomingQueue.poll()

  private def processQsoMessage(
      udpHeader: UDPHeaderData
    ): Unit =
    receivedQsoCount.inc(1)
    val sJson = new String(udpHeader.payload, "UTF-8")
    decode[Qso](sJson) match
      case Right(qso) =>
        logger.debug(s"Received QSO via multicast: ${qso.callsign}")
        qsoStore.add(qso)
      case Left(error) =>
        logger.error(s"Failed to decode QSO from multicast: $sJson", error)

  private def qsoStore: QsoStore =
    qsoStoreProvider.get()

  thread.setDaemon(true)
  logger.debug("Starting NodeStatusHandler Thread")
  thread.start()

  private val receivedPacketCount = metrics.counter("received_packets")
  private val statusMessageCount = metrics.counter("status_count")
  private val statusPackets = metrics.counter("status_packets")
  private val sendStatusCount = metrics.counter("send_status_count")
  private val receivedQsoCount = metrics.counter("received_qso_count")
