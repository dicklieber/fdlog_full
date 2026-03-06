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
import fdswarm.fx.contest.{ContestConfig, ContestManager}
import fdswarm.model.Qso
import fdswarm.store.ReplicationSupport
import fdswarm.util.{HostAndPortProvider, NodeIdentity}
import io.circe.syntax.*
import fdswarm.util.JavaTimeCirce.given
import io.circe.parser.decode
import io.micrometer.core.instrument.MeterRegistry
import jakarta.inject.{Inject, Singleton}

import java.net.http.HttpClient
@Singleton
class NodeStatusHandler @Inject()(replicationSupport: ReplicationSupport,
                                  statusProcessor: StatusProcessor,
                                  multicastTransport: MulticastTransport,
                                  hostAndPortProvider: HostAndPortProvider,
                                  swarmStatus: SwarmStatus,
                                  contestManager: ContestManager,
                                  meterRegistry: MeterRegistry) extends LazyLogging:
  logger.debug("Starting NodeStatusHandler")
  private val statusCounter = meterRegistry.counter("fdswarm_received_status_total")
  private val qsoCounter = meterRegistry.counter("fdswarm_received_qso_total")

  private val httpClient = HttpClient.newBuilder()
    .followRedirects(HttpClient.Redirect.NORMAL)
    .build()

  private val thread = new Thread(() =>
    while !Thread.currentThread().isInterrupted do
      try
        val udpHeader: UDPHeaderData = multicastTransport.queue.take()
        udpHeader.service match
          case Service.Status =>
            statusCounter.increment()
            val statusMessage = StatusMessage(udpHeader.payload)
            val nodeStuff = NodeStuff(statusMessage, udpHeader.nodeIdentity)
            swarmStatus.put(nodeStuff)
            logger.trace("StatusHandle: StatusMessage  {}.", statusMessage)
            statusProcessor.processStatus(nodeStuff).unsafeRunAndForget()
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
            val configBytes = contestManager.config.asJson.noSpaces.getBytes("UTF-8")
            multicastTransport.send(Service.DiscResponse, configBytes)
          case Service.DiscResponse =>
            // Handled by listeners in ContestDiscovery, ignore here
            logger.trace(s"Received ContestDiscoveryResponse from ${udpHeader.nodeIdentity} (ignoring in NodeStatusHandler)")
      catch
        case _: InterruptedException => Thread.currentThread().interrupt()
        case e: Exception =>
          logger.error("Error in Repl processing loop", e)
    , "Repl-Processor")
  thread.setDaemon(true)
  logger.debug("Starting NodeStatusHandler Thread")
  thread.start()
