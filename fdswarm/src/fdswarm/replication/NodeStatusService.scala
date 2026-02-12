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
import com.organization.BuildInfo
import com.typesafe.scalalogging.LazyLogging
import java.net.{DatagramPacket, DatagramSocket, InetAddress}

@Singleton
class NodeStatusService @Inject()(
    repl: Repl,
    @Named("fdswarm.statusPort") statusPort: Int,
    @Named("fdswarm.broadcastAddress") broadcastAddress: String,
    @Named("fdswarm.broadcastPeriodSec") broadcastPeriodSec: Int
) extends LazyLogging:

  private var socket: Option[DatagramSocket] = None
  private var thread: Option[Thread] = None

  def start(): Unit =
    logger.info(s"Starting NodeStatus broadcaster (every $broadcastPeriodSec s to $broadcastAddress:$statusPort)")
    val s = new DatagramSocket()
    s.setBroadcast(true)
    socket = Some(s)

    val t = new Thread(() =>
      while !Thread.currentThread().isInterrupted do
        try
          val gzipBytes = repl.byFdHourJsonGzip
          val bytes = UDPHeader(Service.Status, gzipBytes)
          
          val address = InetAddress.getByName(broadcastAddress)
          val packet = new DatagramPacket(bytes, bytes.length, address, statusPort)
          s.send(packet)
          logger.trace(s"Broadcasted status: ${bytes.length} bytes")
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
    thread = Some(t)

  def stop(): Unit =
    logger.info("Stopping NodeStatus broadcaster")
    thread.foreach(_.interrupt())
    socket.foreach(_.close())
    thread = None
    socket = None
