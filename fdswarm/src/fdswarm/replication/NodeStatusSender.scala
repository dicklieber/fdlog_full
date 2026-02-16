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

import com.google.inject.{Inject, Singleton}
import com.google.inject.name.Named
import com.typesafe.scalalogging.LazyLogging
import fdswarm.store.QsoStore
import fdswarm.util.{HostAndPort, HostAndPortProvider}

import java.net.{DatagramPacket, DatagramSocket, InetAddress}
import scala.compiletime.uninitialized


/**
 * Periodically broadcasts, via the [[MulticastTransport]] the current node's status to other nodes in the swarm.
 *
 * This service runs a background daemon thread that periodically (every `broadcastPeriodSec`)
 * fetches a gzipped JSON representation of the local hourly QSO digests from [[NodeStatusHandler]]
 * and broadcasts it as a UDP packet to the configured `broadcastAddress` and `statusPort`.
 *
 * The UDP packets are prefixed with a standard [[UDPHeader]] with [[Service.Status]].
 *
 * one */
@Singleton
class NodeStatusSender @Inject()(
                                  qsoStore: QsoStore,
                                  multicastTransport: MulticastTransport,
                                  hostAndPortProvider: HostAndPortProvider,
                                  @Named("fdswarm.broadcastPeriodSec") broadcastPeriodSec: Int
                                ) extends LazyLogging:
  var maybeThread:Option[Thread] = None

  def start(): Unit =
    logger.info(s"Starting NodeStatusSender (every $broadcastPeriodSec)")

    val t = new Thread(() =>
      while !Thread.currentThread().isInterrupted do
        try
          val statusMessage = StatusMessage(
            hostAndPort = hostAndPortProvider.http,
            fdDigests = qsoStore.digests())
          logger.trace(s"Broadcasting: $statusMessage")
          val gzipBytes = statusMessage.toPacket
          val bytes = UDPHeader(Service.Status, gzipBytes)
          multicastTransport.send(bytes)
        catch
          case _: InterruptedException => Thread.currentThread().interrupt()
          case e: Exception =>
            logger.error("Error broadcasting node status", e)

        if !Thread.currentThread().isInterrupted then
          try
            Thread.sleep(broadcastPeriodSec * 1000L)
          catch
            case _: InterruptedException => Thread.currentThread().interrupt()
      , "NodeStatus-Broadcaster")
    t.setDaemon(true)
    t.start()
    maybeThread = Some(t)

  def stop(): Unit =
    logger.info("Stopping NodeStatusSender")
    maybeThread.foreach(_.interrupt())
    maybeThread = None