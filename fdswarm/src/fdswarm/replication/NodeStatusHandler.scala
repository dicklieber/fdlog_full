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
import fdswarm.replication.status.{SwarmData, SwarmStatus}
import fdswarm.store.ReplicationSupport
import io.circe.generic.auto.deriveDecoder
import io.circe.parser.decode
import io.micrometer.core.instrument.MeterRegistry
import jakarta.inject.{Inject, Provider, Singleton}
import scalafx.application.Platform

import java.net.http.HttpClient
@Singleton
class NodeStatusHandler @Inject()(replicationSupportProvider: Provider[ReplicationSupport],
                                  statusProcessor: StatusProcessor,
                                  swarmData:SwarmData,
                                  transport: Transport,
                                  statusBroadcastService: StatusBroadcastService,
                                  swarmStatusProvider: Provider[SwarmStatus],
                                  contestManagerProvider: Provider[ContestConfigManager],
                                  meterRegistry: MeterRegistry) extends LazyLogging:

  private def replicationSupport: ReplicationSupport = replicationSupportProvider.get()
  private def swarmStatus: SwarmStatus = swarmStatusProvider.get()
  private def contestManager: ContestConfigManager = contestManagerProvider.get()
  logger.debug("Starting NodeStatusHandler")
  private val sendStatusReceived = meterRegistry.counter("fdswarm_discovery_req_received")
  private val statusCounter = meterRegistry.counter("fdswarm_received_status_total")
  private val qsoCounter = meterRegistry.counter("fdswarm_received_qso_total")
  private var lastStatusMessagePayloadSize: Double = 0.0
  private var lastStatusMessageDigestCount: Int = 0

  meterRegistry.gauge("fdswarm_received_status_payload_bytes", this, (handler: NodeStatusHandler) => handler.lastStatusMessagePayloadSize)
  meterRegistry.gauge("fdswarm_received_status_digest_count", this, (handler: NodeStatusHandler) => handler.lastStatusMessageDigestCount.toDouble)

  private val statusQueue = transport.startQueue(Service.Status)
  private val qsoQueue = transport.startQueue(Service.QSO)
  private val restartContestQueue = transport.startQueue(Service.RestartContest)

  private val httpClient = HttpClient.newBuilder()
    .followRedirects(HttpClient.Redirect.NORMAL)
    .build()

  private val thread = new Thread(() =>
    while !Thread.currentThread().isInterrupted do
      try
        val udpHeader: UDPHeaderData = statusQueue.take()
        udpHeader.service match
          case Service.Status =>
            val statusMessage = StatusMessage(udpHeader.payload)
            if (contestManager.shouldIgnoreStatus) 
              logger.debug(s"Ignoring status message from ${udpHeader.nodeIdentity} because of recent contest change")
            else 
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
            sendStatusReceived.increment()
            if !contestManager.hasConfiguration.value then
              logger.debug(
                "Received SendStatus from {} but contest config is not initialized yet",
                udpHeader.nodeIdentity
              )
            else
              statusBroadcastService.broadcastStatus(force = true)
              logger.trace(
                "Received SendStatus from {} and broadcasted StatusMessage",
                udpHeader.nodeIdentity
              )
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
          logger.error("Error in Repl processing loop", e)
    , "Repl-Processor")
  thread.setDaemon(true)
  logger.debug("Starting NodeStatusHandler Thread")
  thread.start()
