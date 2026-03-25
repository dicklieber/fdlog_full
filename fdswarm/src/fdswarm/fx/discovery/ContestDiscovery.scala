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

package fdswarm.fx.discovery

import com.google.inject.name.Named
import com.google.inject.Provider
import com.typesafe.scalalogging.LazyLogging
import fdswarm.StationConfigManager
import fdswarm.fx.contest.{ContestConfig, ContestConfigManager}
import fdswarm.model.StationConfig
import fdswarm.replication.{LiveOrDeadQueue, Service, Transport, UDPHeaderData}
import fdswarm.util.NodeIdentity
import io.circe.Codec
import io.circe.parser.decode
import io.circe.syntax._
import io.micrometer.core.instrument.MeterRegistry
import java.nio.charset.StandardCharsets
import jakarta.inject.{Inject, Singleton}

import java.util.concurrent.{ConcurrentHashMap, CountDownLatch, TimeUnit}
import scala.jdk.CollectionConverters.*

class ContestDiscovery @Inject() (
    val transport: Transport,
    contestConfigProvider: Provider[ContestConfigManager],
    stationConfigManager: StationConfigManager,
    @Named("fdswarm.contestDiscoveryTimeoutSec") val timeoutSec: Int,
    meterRegistry: MeterRegistry
) extends LazyLogging:

  private val discReqReceived = meterRegistry.counter("fdswarm_discovery_req_received")

  private val queue: LiveOrDeadQueue = transport.startQueue(Service.DiscReq)
  // This thread runbs for ever handling Service.DiscReq messages
  new Thread {
    setDaemon(true)
    setName("ContestDiscoveryHandler")
    override def run(): Unit = {
      while (true) {
        val msg: UDPHeaderData = queue.take()
        discReqReceived.increment()
        val response = DiscoveryWire(contestConfigProvider.get.config, stationConfigManager.station)
        val jsonBytes = response.asJson.noSpaces.getBytes(StandardCharsets.UTF_8)
        transport.send(Service.DiscResponse, jsonBytes)
        logger.trace("Received DiscReq from {} responding with {}",msg.nodeIdentity, response)
      }
    }
  }.start()
  
  def discoverContest(
                       onResponse: NodeContestStation => Unit 
                     ): Unit =
    logger.info(s"Starting contest discovery (timeout: ${timeoutSec}s)")

    val responseQueue = transport.startQueue(Service.DiscReq, Service.DiscResponse)
    val startTime = System.currentTimeMillis()
    while (System.currentTimeMillis() - startTime < timeoutSec * 1000L) {
      responseQueue.poll(100, TimeUnit.MILLISECONDS) match {
        case null =>
        case udpHeaderData =>
          logger.info(s"Discovery handler: ${udpHeaderData.nodeIdentity} service=${udpHeaderData.service}")
          if udpHeaderData.service == Service.DiscResponse then
            try
              val xx: DiscoveryWire = udpHeaderData.decode
              val disMessage = NodeContestStation(udpHeaderData.nodeIdentity, xx)
              onResponse(disMessage)
              logger.info(s"Processed DiscResponse from ${udpHeaderData.nodeIdentity}")
            catch
              case e: Exception =>
                logger.error(s"Failed to process DiscResponse from ${udpHeaderData.nodeIdentity}", e)
      }
    }
    transport.stopQueue(Service.DiscResponse)


/**
 * What gets sent in a UDP packet of type [[fdswarm.replication.Service.DiscResponse]]
 * in response to a [[fdswarm.replication.Service.DiscReq]].
 *
 */
case class DiscoveryWire(contestConfig: ContestConfig, stationConfig: StationConfig) derives Codec.AsObject

/**
 * Combines the ContestStation as received from other nodes with the NodeIdentity of the node, as extracted from the UDPHeader..
 */
case class NodeContestStation(nodeIdentity: NodeIdentity, contestStation: DiscoveryWire) derives Codec.AsObject