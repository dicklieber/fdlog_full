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
import fdswarm.fx.qso.FdHour
import fdswarm.model.Qso
import fdswarm.store.{FdHourDigest, QsoStore, ReplicationSupport}
import fdswarm.util.HostAndPortProvider
import io.circe.parser.decode
import io.micrometer.core.instrument.MeterRegistry
import jakarta.inject.Inject

import java.net.URI
import java.net.http.HttpResponse.BodyHandlers
import java.net.http.{HttpClient, HttpRequest}

class NodeStatusHandler @Inject()(replicationSupport: ReplicationSupport,
                                  statusProcessor: StatusProcessor,
                                  multicastTransport: MulticastTransport,
                                  hostAndPortProvider: HostAndPortProvider,
                                  swarmStatus: SwarmStatus,
                                  meterRegistry: MeterRegistry) extends LazyLogging:

  private val statusCounter = meterRegistry.counter("fdswarm_received_status_total")
  private val qsoCounter = meterRegistry.counter("fdswarm_received_qso_total")

  private val httpClient = HttpClient.newBuilder()
    .followRedirects(HttpClient.Redirect.NORMAL)
    .build()

  private var thread: Option[Thread] = None

  def start(): Unit =
    val t = new Thread(() =>
      while !Thread.currentThread().isInterrupted do
        try
          val udpHeader: UDPHeaderData = multicastTransport.queue.take()
          udpHeader.service match
            case Service.Status =>
              statusCounter.increment()
              val statusMessage = StatusMessage(udpHeader.payload)
              swarmStatus.put(statusMessage)
              logger.trace("StatusHandle: StatusMessage from {} with {} digests.", statusMessage.hostAndPort, statusMessage.fdDigests.size)
              statusProcessor.processStatus(statusMessage).unsafeRunSync()
            case Service.QSO =>
              qsoCounter.increment()
              val json = new String(udpHeader.payload, "UTF-8")
              decode[Qso](json) match
                case Right(qso) =>
                  logger.debug(s"Received QSO via multicast: ${qso.callsign}")
                  replicationSupport.add(qso)
                case Left(error) =>
                  logger.error(s"Failed to decode QSO from multicast: $json", error)
        catch
          case _: InterruptedException => Thread.currentThread().interrupt()
          case e: Exception =>
            logger.error("Error in Repl processing loop", e)
      , "Repl-Processor")
    t.setDaemon(true)
    t.start()
    thread = Some(t)

  def stop(): Unit =
    logger.debug("Stopping Repl service")
    thread.foreach(_.interrupt())
    thread = None

 