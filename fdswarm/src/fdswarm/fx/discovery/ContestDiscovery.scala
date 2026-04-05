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
import fdswarm.fx.contest.ContestConfig
import fdswarm.fx.station.StationConfig
import fdswarm.replication.{ReceivedNodeStatus, Service, Transport}
import fdswarm.replication.status.SwarmStatus
import io.circe.Codec
import jakarta.inject.Inject

import java.util.concurrent.TimeUnit

class ContestDiscovery @Inject()(
                                  val transport: Transport,
                                  swarmStatus: SwarmStatus,
                                  @Named("fdswarm.contestDiscoveryTimeoutSec") val timeoutSec: Int,
                                ) extends LazyLogging:

  def discoverContest(): Seq[ReceivedNodeStatus] =
    logger.info(s"Starting contest discovery (timeout: ${timeoutSec}s)")

    transport.send(Service.SendStatus, Array.emptyByteArray)
    TimeUnit.SECONDS.sleep(timeoutSec.toLong)

    val localNodeIdentity = swarmStatus.ourNodeIdentity
    val discoveredNodes = swarmStatus.nodeMap.values
      .filterNot(_.nodeIdentity == localNodeIdentity)
      .toSeq

    discoveredNodes.foreach { discovered =>
      logger.info(s"Processed StatusMessage from ${discovered.nodeIdentity}")
    }
    discoveredNodes

