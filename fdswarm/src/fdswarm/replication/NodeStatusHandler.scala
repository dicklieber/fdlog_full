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

import cats.effect.unsafe.implicits.global
import com.typesafe.scalalogging.LazyLogging
import fdswarm.fx.contest.{ContestConfig, ContestConfigManager}
import fdswarm.model.Qso
import fdswarm.replication.status.SwarmData
import fdswarm.store.ReplicationSupport
import io.circe.generic.auto.deriveDecoder
import io.circe.parser.decode
import io.micrometer.core.instrument.MeterRegistry
import jakarta.inject.{Inject, Provider, Singleton}
import scalafx.application.Platform

import java.net.http.HttpClient

/**
 * Handles node status updates, status broadcasts, QSO processing, and contest restarts.
 * This class is responsible for managing the processing of incoming UDP packets
 * related to various services, such as node status, QSO, and contest configurations.
 * It facilitates the updating of swarm data, processing of status messages, and
 * communication with other components for replication and status broadcast.
 *
 * Dependencies are injected to enable functionality for swarm data updates, status processing,
 * replication support, contest configuration management, and metrics instrumentation.
 *
 * Thread-based processing ensures continuous handling of incoming requests without blocking
 * other operations.
 *
 * Constructor parameters:
 * - `replicationSupportProvider`: Facilitates replication-related tasks, such as checking and adding QSOs.
 * - `statusProcessor`: Processes and handles incoming status messages.
 * - `swarmData`: Maintains information about node statuses in the swarm.
 * - `transport`: Enables communication through queues for handling various service-related messages.
 * - `statusBroadcastService`: Manages the broadcasting of local node statuses to other nodes.
 * - `contestManagerProvider`: Provides access to contest configuration management.
 * - `meterRegistry`: Instrumentation for registering and tracking metrics throughout the handler.
 */
@Singleton
class NodeStatusHandler @Inject()(replicationSupportProvider: Provider[ReplicationSupport],
                                  statusProcessor: StatusProcessor,
                                  swarmData:SwarmData,
                                  transport: Transport,
                                  statusBroadcastService: StatusBroadcastService,
                                  contestManagerProvider: Provider[ContestConfigManager],
                                  meterRegistry: MeterRegistry) extends LazyLogging:

  private val sendStatusReceived = meterRegistry.counter("fdswarm_discovery_req_received")
  private val statusCounter = meterRegistry.counter("fdswarm_received_status_total")
  logger.debug("Starting NodeStatusHandler")
  private val qsoCounter = meterRegistry.counter("fdswarm_received_qso_total")
  private val httpClient = HttpClient.newBuilder()
    .followRedirects(HttpClient.Redirect.NORMAL)
    .build()

  meterRegistry.gauge("fdswarm_received_status_payload_bytes", this, (handler: NodeStatusHandler) => handler.lastStatusMessagePayloadSize)
  meterRegistry.gauge("fdswarm_received_status_digest_count", this, (handler: NodeStatusHandler) => handler.lastStatusMessageDigestCount.toDouble)
  private val thread = new Thread(() =>
    while !Thread.currentThread().isInterrupted do
      try
        val udpHeader: UDPHeaderData = transport.incomingQueue.take()
        logger.trace(s"Received UDP packet from ${udpHeader.nodeIdentity} for service ${udpHeader.service}")
        udpHeader.service match
          case Service.Status =>
            val statusMessage = StatusMessage(udpHeader.payload)
            statusCounter.increment()
            lastStatusMessagePayloadSize = udpHeader.payload.length.toDouble
            lastStatusMessageDigestCount = statusMessage.fdDigests.size
            val nodeStatus = NodeStatus(statusMessage, udpHeader.nodeIdentity, isLocal = false)
            swarmData.update(nodeStatus)
            logger.trace("nodeStatus:  {}.", nodeStatus)
            //              swarmStatusProviders.put(receivedNodeStatus)
            statusProcessor.processStatus(nodeStatus).unsafeRunAndForget()
          case Service.QSO =>
            qsoCounter.increment()
            val sJson = new String(udpHeader.payload, "UTF-8")
            decode[Qso](sJson) match
              case Right(qso) =>
                logger.debug(s"Received QSO via multicast: ${qso.callsign}")
                replicationSupport.add(qso)
              case Left(error) =>
                logger.error(s"Failed to decode QSO from multicast: $sJson", error)
          case Service.SendStatus =>
            logger.info(s"Received SendStatus from ${udpHeader.nodeIdentity}")
            sendStatusReceived.increment()
            statusBroadcastService.broadcastStatus()

          case Service.RestartContest =>
            logger.info(s"Received RestartContest from ${udpHeader.nodeIdentity}")
            val sJson = new String(udpHeader.payload, "UTF-8")
            decode[ContestConfig](sJson) match
              case Right(newConfig) =>
                // ContestConfigManager exposes JavaFX properties; update them on the FX thread.
                Platform.runLater {
                  contestManager.handleRestartContest(newConfig)
                }
              case Left(error) =>
                logger.error(s"Failed to decode ContestConfig from RestartContest: $sJson", error)
      catch
        case _: InterruptedException => Thread.currentThread().interrupt()

         case e: Exception =>
           e.printStackTrace()
           logger.error(s"Error in Repl processing loop ${e.getMessage}", e)
    , "Repl-Processor")
  private var lastStatusMessagePayloadSize: Double = 0.0
  private var lastStatusMessageDigestCount: Int = 0

  private def replicationSupport: ReplicationSupport = replicationSupportProvider.get()

  private def contestManager: ContestConfigManager = contestManagerProvider.get()
  thread.setDaemon(true)
  logger.debug("Starting NodeStatusHandler Thread")
  thread.start()
