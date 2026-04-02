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
import fdswarm.fx.contest.{ContestConfig, ContestConfigManager}
import fdswarm.model.StationConfig
import fdswarm.replication.{LiveOrDeadQueue, Service, StatusBroadcastService, StatusMessage, Transport, UDPHeaderData}
import io.circe.Codec
import io.micrometer.core.instrument.MeterRegistry
import jakarta.inject.Inject

import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

class ContestDiscovery @Inject()(
                                  val transport: Transport,
                                  contestConfigManager: ContestConfigManager,
                                  statusBroadcastService: StatusBroadcastService,
                                  @Named("fdswarm.contestDiscoveryTimeoutSec") val timeoutSec: Int,
                                  meterRegistry: MeterRegistry
                                ) extends LazyLogging:

  private val sendStatusReceived =
    meterRegistry.counter("fdswarm_discovery_req_received")
  private val discoveredStatusQueue = new LinkedBlockingQueue[NodeContestStation]()

  private val requestQueue: LiveOrDeadQueue =
    transport.startQueue(Service.SendStatus)

  // This thread runs forever handling Service.SendStatus messages.
  val t: Thread = new Thread:
    setDaemon(true)
    setName("ContestDiscoveryHandler")

    override def run(): Unit =
      while true do
        val msg: UDPHeaderData = requestQueue.take()
        sendStatusReceived.increment()

        if !contestConfigManager.hasConfiguration.value then
          logger.debug(
            "Received SendStatus from {} but contest config is not initialized yet",
            msg.nodeIdentity
          )
        else
          statusBroadcastService.broadcastStatus(force = true)
          logger.trace(
            "Received SendStatus from {} and broadcasted StatusMessage",
            msg.nodeIdentity
          )
  t.start()

  def onStatusMessageReceived(udpHeaderData: UDPHeaderData, statusMessage: StatusMessage): Unit =
    discoveredStatusQueue.offer(NodeContestStation.fromStatus(udpHeaderData, statusMessage))

  def discoverContest(callBack: NodeContestStation => Unit): Unit =
    logger.info(s"Starting contest discovery (timeout: ${timeoutSec}s)")

    discoveredStatusQueue.clear()
    transport.send(Service.SendStatus, Array.emptyByteArray)

    val startTime = System.currentTimeMillis()

    while System.currentTimeMillis() - startTime < timeoutSec * 1000L do
      discoveredStatusQueue.poll(100, TimeUnit.MILLISECONDS) match
        case null =>
        case discovered =>
          callBack(discovered)
          logger.info(
            s"Processed StatusMessage from ${discovered.nodeIdentity}"
          )

/**
 * What discovery UIs and startup checks consume from a remote node.
 */
case class DiscoveryWire(contestConfig: ContestConfig, stationConfig: StationConfig)

object DiscoveryWire:
  given Codec.AsObject[DiscoveryWire] = Codec.AsObject.derived
