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
import fdswarm.replication.{MulticastTransport, Service, UDPHeaderData}
import fdswarm.util.NodeIdentity
import io.circe.parser.decode
import jakarta.inject.{Inject, Singleton}

import java.util.concurrent.{ConcurrentHashMap, CountDownLatch, TimeUnit}
import scala.jdk.CollectionConverters.*

@Singleton
class ContestDiscovery @Inject() (
    multicastTransport: MulticastTransport,
    @Named("fdswarm.contestDiscoveryTimeoutSec") val timeoutSec: Int
) extends LazyLogging:

  def discoverContest(
      onResponse: (NodeIdentity, ContestConfig) => Unit = (_, _) => ()
  ): Map[NodeIdentity, ContestConfig] =
    val latch = new CountDownLatch(1)
    logger.info(s"Starting contest discovery (timeout: ${timeoutSec}s)")
    val responses = new ConcurrentHashMap[NodeIdentity, ContestConfig]()

    val handler: UDPHeaderData => Unit = (udpHeader: UDPHeaderData) =>
      if udpHeader.service == Service.DiscResponse then
        val sJson = new String(udpHeader.payload, "UTF-8")
        decode[ContestConfig](sJson) match
          case Right(config) =>
            logger.debug(
              s"Received ContestConfig from ${udpHeader.nodeIdentity}"
            )
            responses.put(udpHeader.nodeIdentity, config)
            onResponse(udpHeader.nodeIdentity, config)
          case Left(error) =>
            logger.error(
              s"Failed to decode ContestConfig from ${udpHeader.nodeIdentity}: $sJson",
              error
            )

    multicastTransport.addListener(handler)
    try
      multicastTransport.send(Service.DiscReq, Array.emptyByteArray)
      latch.await(timeoutSec, TimeUnit.SECONDS)
    finally multicastTransport.removeListener(handler)

    responses.asScala.toMap
