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

import fdswarm.model.Qso
import fdswarm.store.{FdHourDigest, QsoStore}
import jakarta.inject.Inject
import upickle.default.*

import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.zip.GZIPOutputStream
import scala.collection.immutable
import com.typesafe.scalalogging.LazyLogging
import fdswarm.fx.qso.FdHour
import fdswarm.util.Ids.Id

class Repl @Inject()(qsoStore: QsoStore, nodeStatusReceiver: NodeStatusReceiver) extends LazyLogging:

  private var thread: Option[Thread] = None

  def start(): Unit =
    val t = new Thread(() =>
      while !Thread.currentThread().isInterrupted do
        try
          val payload = nodeStatusReceiver.queue.take()
          val statusMessage = StatusMessage(payload)
          val needed = qsoStore.neededQsos(statusMessage.fdDigests)
          if needed.nonEmpty then
            logger.info(s"Needed FdHours from ${statusMessage.hostAndPort}: $needed")
        catch
          case _: InterruptedException => Thread.currentThread().interrupt()
          case e: Exception =>
            logger.error("Error in Repl processing loop", e)
    , "Repl-Processor")
    t.setDaemon(true)
    t.start()
    thread = Some(t)

  def stop(): Unit =
    logger.info("Stopping Repl service")
    thread.foreach(_.interrupt())
    thread = None

 