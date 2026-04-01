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
import com.typesafe.scalalogging.LazyLogging
import fdswarm.StationConfigManager
import fdswarm.fx.contest.{ContestConfig, ContestConfigManager}
import fdswarm.model.StationConfig
import fdswarm.replication.{LiveOrDeadQueue, Service, Transport, UDPHeaderData}
import fdswarm.util.NodeIdentity
import io.circe.Codec
import io.circe.syntax.*
import io.micrometer.core.instrument.MeterRegistry
import jakarta.inject.Inject

import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

class ContestDiscovery @Inject()(
                                  val transport: Transport,
                                  contestConfigManager: ContestConfigManager,
                                  stationConfigManager: StationConfigManager,
                                  @Named("fdswarm.contestDiscoveryTimeoutSec") val timeoutSec: Int,
                                  meterRegistry: MeterRegistry
                                ) extends LazyLogging:

  private val discReqReceived =
    meterRegistry.counter("fdswarm_discovery_req_received")

  private val requestQueue: LiveOrDeadQueue =
    transport.startQueue(Service.DiscReq)

  // This thread runs forever handling Service.DiscReq messages.
  val t: Thread = new Thread:
    setDaemon(true)
    setName("ContestDiscoveryHandler")

    override def run(): Unit =
      while true do
        val msg: UDPHeaderData = requestQueue.take()
        discReqReceived.increment()

        if !contestConfigManager.hasConfiguration.value then
          logger.debug(
            "Received DiscReq from {} but contest config is not initialized yet",
            msg.nodeIdentity
          )
        else
          val contestConfig: ContestConfig =
            contestConfigManager.contestConfigProperty.value

          val response =
            DiscoveryWire(contestConfig, stationConfigManager.station)

          val jsonBytes =
            response.asJson.noSpaces.getBytes(StandardCharsets.UTF_8)

          transport.send(Service.DiscResponse, jsonBytes)
          logger.trace(
            "Received DiscReq from {} responding with {}",
            msg.nodeIdentity,
            response
          )
  t.start()

  def discoverContest(callBack: NodeContestStation => Unit): Unit =
    logger.info(s"Starting contest discovery (timeout: ${timeoutSec}s)")

    val responseQueue: LiveOrDeadQueue =
      transport.startQueue(Service.DiscReq, Service.DiscResponse)

    val startTime = System.currentTimeMillis()

    while System.currentTimeMillis() - startTime < timeoutSec * 1000L do
      responseQueue.poll(100, TimeUnit.MILLISECONDS) match
        case null =>
        case udpHeaderData =>
          logger.info(
            s"Discovery handler: ${udpHeaderData.nodeIdentity} service=${udpHeaderData.service}"
          )

          if udpHeaderData.service == Service.DiscResponse then
            try
              val wire: DiscoveryWire = udpHeaderData.decode[DiscoveryWire]
              val nodeContestStation =
                NodeContestStation(udpHeaderData.nodeIdentity, wire)
              callBack(nodeContestStation)
              logger.info(
                s"Processed DiscResponse from ${udpHeaderData.nodeIdentity}"
              )
            catch
              case e: Exception =>
                logger.error(
                  s"Failed to process DiscResponse from ${udpHeaderData.nodeIdentity}",
                  e
                )

    transport.stopQueue(Service.DiscResponse)

/**
 * What gets sent in a UDP packet of type [[fdswarm.replication.Service.DiscResponse]]
 * in response to a [[fdswarm.replication.Service.DiscReq]].
 */
case class DiscoveryWire(contestConfig: ContestConfig, stationConfig: StationConfig)
  derives Codec.AsObject

/**
 * Combines the ContestStation as received from other nodes with the NodeIdentity
 * of the node, as extracted from the UDPHeader.
 */
case class NodeContestStation(nodeIdentity: NodeIdentity, discoveryWire: DiscoveryWire)
  derives Codec.AsObject:

  val exchange: String =
    discoveryWire.contestConfig.exchange
