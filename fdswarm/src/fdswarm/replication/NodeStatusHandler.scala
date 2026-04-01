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
import fdswarm.replication.status.SwarmStatus
import fdswarm.store.ReplicationSupport
import fdswarm.util.JavaTimeCirce.given
import fdswarm.util.{InstanceIdManager, NodeIdentity, NodeIdentityManager}
import fdswarm.StationConfigManager
import fdswarm.fx.discovery.DiscoveryWire
import io.circe.parser.decode
import io.circe.syntax.*
import io.micrometer.core.instrument.MeterRegistry
import jakarta.inject.{Inject, Singleton}
import java.util.concurrent.TimeUnit

import java.net.http.HttpClient
@Singleton
class NodeStatusHandler @Inject()(replicationSupport: ReplicationSupport,
                                  statusProcessor: StatusProcessor,
                                  transport: Transport,
                                  nodeIdentityManager: NodeIdentityManager,
                                  swarmStatus: SwarmStatus,
                                  contestManager: ContestConfigManager,
                                  stationManager: StationConfigManager,
                                  instanceIdManager: InstanceIdManager,
                                  meterRegistry: MeterRegistry) extends LazyLogging:
  logger.debug("Starting NodeStatusHandler")
  private val statusCounter = meterRegistry.counter("fdswarm_received_status_total")
  private val qsoCounter = meterRegistry.counter("fdswarm_received_qso_total")
  private var lastStatusMessagePayloadSize: Double = 0.0
  private var lastStatusMessageDigestCount: Int = 0

  meterRegistry.gauge("fdswarm_received_status_payload_bytes", this, (handler: NodeStatusHandler) => handler.lastStatusMessagePayloadSize)
  meterRegistry.gauge("fdswarm_received_status_digest_count", this, (handler: NodeStatusHandler) => handler.lastStatusMessageDigestCount.toDouble)

  private val statusQueue = transport.startQueue(Service.Status)
  private val qsoQueue = transport.startQueue(Service.QSO)
  private val restartContestQueue = transport.startQueue(Service.RestartContest)
//  private val instanceQueryQueue = transport.startQueue(Service.InstanceQuery)
//  private val instanceRespQueue = transport.startQueue(Service.InstanceResponse)

  private val httpClient = HttpClient.newBuilder()
    .followRedirects(HttpClient.Redirect.NORMAL)
    .build()

  private val thread = new Thread(() =>
    while !Thread.currentThread().isInterrupted do
      try
        val udpHeader: UDPHeaderData = statusQueue.take()
        udpHeader.service match
          case Service.Status =>
            if (contestManager.shouldIgnoreStatus) 
              logger.debug(s"Ignoring status message from ${udpHeader.nodeIdentity} because of recent contest change")
            else 
              statusCounter.increment()
              lastStatusMessagePayloadSize = udpHeader.payload.length.toDouble
              val statusMessage = StatusMessage(udpHeader.payload)
              lastStatusMessageDigestCount = statusMessage.fdDigests.size
              val receivedNodeStatus = ReceivedNodeStatus(statusMessage, udpHeader.nodeIdentity)
              swarmStatus.put(receivedNodeStatus)
              logger.trace("StatusHandle: StatusMessage  {}.", statusMessage)
              statusProcessor.processStatus(receivedNodeStatus).unsafeRunAndForget()
          case Service.QSO =>
            qsoCounter.increment()
            val sJson = new String(udpHeader.payload, "UTF-8")
            decode[Qso](sJson) match
              case Right(qso) =>
                logger.debug(s"Received QSO via multicast: ${qso.callsign}")
                replicationSupport.add(qso)
              case Left(error) =>
                logger.error(s"Failed to decode QSO from multicast: $sJson", error)
          case Service.DiscReq =>
            logger.debug(s"Received ContestDiscoveryRequest from ${udpHeader.nodeIdentity}")
            if !contestManager.hasConfiguration.value then
              logger.debug(
                s"Skipping ContestDiscoveryResponse to ${udpHeader.nodeIdentity} because contest config is not initialized"
              )
            else
              val contestStation = DiscoveryWire(contestManager.contestConfigProperty.value, stationManager.station)
              val configBytes = contestStation.asJson.noSpaces.getBytes("UTF-8")
              transport.send(Service.DiscResponse, configBytes)
          case Service.DiscResponse =>
            // Handled by listeners in ContestDiscovery, ignore here
            logger.trace(s"Received ContestDiscoveryResponse from ${udpHeader.nodeIdentity} (ignoring in NodeStatusHandler)")
          case Service.RestartContest =>
            logger.info(s"Received RestartContest from ${udpHeader.nodeIdentity}")
            val sJson = new String(udpHeader.payload, "UTF-8")
            decode[ContestConfig](sJson) match
              case Right(newConfig) =>
                contestManager.handleRestartContest(newConfig)
              case Left(error) =>
                logger.error(s"Failed to decode ContestConfig from RestartContest: $sJson", error)
          case Service.InstanceQuery =>
            val requestedInstanceId = new String(udpHeader.payload, "UTF-8")
            if requestedInstanceId == instanceIdManager.ourInstanceId then
              logger.debug(s"Received InstanceQuery for our instance: $requestedInstanceId")
              val responsePayload = nodeIdentityManager.ourNodeIdentity.toString.getBytes("UTF-8")
              transport.send(Service.InstanceResponse, responsePayload)
          case Service.InstanceResponse =>
            logger.trace(s"Received InstanceResponse from ${udpHeader.nodeIdentity}")
      catch
        case _: InterruptedException => Thread.currentThread().interrupt()
        case e: Exception =>
          logger.error("Error in Repl processing loop", e)
    , "Repl-Processor")
  thread.setDaemon(true)
  logger.debug("Starting NodeStatusHandler Thread")
  thread.start()
