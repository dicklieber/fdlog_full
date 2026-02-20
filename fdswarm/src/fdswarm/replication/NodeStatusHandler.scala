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

import com.typesafe.scalalogging.LazyLogging
import fdswarm.fx.qso.FdHour
import fdswarm.store.{FdHourDigest, QsoStore}
import fdswarm.util.HostAndPortProvider
import jakarta.inject.Inject

import java.net.URI
import java.net.http.HttpResponse.BodyHandlers
import java.net.http.{HttpClient, HttpRequest}

class NodeStatusHandler @Inject()(qsoStore: QsoStore,
                                  multicastTransport: MulticastTransport,
                                  hostAndPortProvider: HostAndPortProvider,
                                  swarmStatus:SwarmStatus) extends LazyLogging:

  private val httpClient = HttpClient.newBuilder()
    .followRedirects(HttpClient.Redirect.NORMAL)
    .build()

  private var thread: Option[Thread] = None

  def start(): Unit =
    val t = new Thread(() =>
      while !Thread.currentThread().isInterrupted do
        try
          val payload: Array[Byte] = multicastTransport.queue.take()
          val udpHeader: UDPHeaderData = UDPHeader.parse(payload)
          val statusMessage = StatusMessage(udpHeader.payload)
          swarmStatus.put(statusMessage)
          if statusMessage.hostAndPort == hostAndPortProvider.http then
            logger.trace(s"Ignoring our own message from ${statusMessage.hostAndPort}")
          else
            logger.trace("StatusHandle: StatusMessage from {} with {} digests.", statusMessage.hostAndPort, statusMessage.fdDigests.size)
            val neededFdHours: Seq[FdHour] = {
              val incoming = statusMessage.fdDigests
              val cpy: Map[FdHour, FdHourDigest] = qsoStore.digests().map(d => d.fdHour -> d).toMap
              incoming.flatMap { remoteFdHourDigest =>
                val remoteFdHour = remoteFdHourDigest.fdHour
                cpy.get(remoteFdHour) match
                  // we have one, is it the same?
                  case Some(localFdDigest) =>
                    Option.when(localFdDigest != remoteFdHourDigest) {
                      remoteFdHour
                    }
                  case None => // we don't have it yet, so we need it.
                    Some(remoteFdHour)
              }
            }
            logger.trace("StatusHandle: Needed QSOs: {}",   neededFdHours.size)
            if neededFdHours.nonEmpty then
              neededFdHours.foreach(fdHour =>
                val url = s"http://${statusMessage.hostAndPort}/hourQsos/$fdHour"
                logger.debug(s"StatusHandle: Sending: {}",url)
                try
                  val request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Accept-Encoding", "gzip")
                    .GET()
                    .build()

                  val response =  httpClient.send(request, BodyHandlers.ofString())
                  logger.trace(s"StatusHandle: Response from $url: ${response.statusCode()}")
//                  response.  
//                    .thenAccept(response =>
//                      if response.statusCode() == 200 then
//                        logger.debug(s"Success response from $url: ${response.statusCode()}")
//                      else
//                        logger.warn(s"Response from $url: ${response.statusCode()} body: ${response.body()}")
//                    )
                catch
                  case e: Exception =>
                    logger.error(s"Failed to send FdHour request to $url", e)
              )
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

 