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

package fdswarm.fx.contest

import com.google.inject.name.Named
import com.typesafe.scalalogging.LazyLogging
import fdswarm.model.StationConfig
import fdswarm.replication.{Service, Transport, UDPHeaderData}
import fdswarm.util.NodeIdentity
import io.circe.Codec
import io.circe.parser.decode
import jakarta.inject.{Inject, Singleton}

import java.util.concurrent.{ConcurrentHashMap, CountDownLatch, TimeUnit}
import scala.jdk.CollectionConverters.*

@Singleton
class ContestDiscovery @Inject() (
    transport: Transport,
    @Named("fdswarm.contestDiscoveryTimeoutSec") val timeoutSec: Int
) extends LazyLogging:

  def discoverContest(
                       onResponse: NodeContestStation => Unit 
                     ): Unit =
    val latch = new CountDownLatch(1)
    logger.info(s"Starting contest discovery (timeout: ${timeoutSec}s)")

    val handler: UDPHeaderData => Unit = (udpHeaderData: UDPHeaderData) =>
      require(udpHeaderData.service == Service.DiscResponse)

      val xx: DiscoveryWire = udpHeaderData.decode
      val disMessage = NodeContestStation(udpHeaderData.nodeIdentity, xx)
      onResponse(disMessage)
    // start listening for responses before sending the request.
    transport.addListener(handler)
    transport.send(Service.DiscReq)
    latch.await(timeoutSec, TimeUnit.SECONDS)
    transport.removeListener(handler)


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