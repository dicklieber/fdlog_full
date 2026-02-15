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

import fdswarm.model.{FdHour, Qso}
import fdswarm.store.{FdHourDigest, QsoStore}
import jakarta.inject.Inject
import upickle.default.*

import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.zip.GZIPOutputStream
import scala.collection.immutable
import com.typesafe.scalalogging.LazyLogging
import fdswarm.util.Ids.Id

class Repl @Inject()(qsoStore: QsoStore, nodeStatusReceiverService: NodeStatusReceiverService) extends LazyLogging:

  private var thread: Option[Thread] = None

  def start(): Unit =
    if thread.isEmpty then
      val t = new Thread(() =>
        logger.info("Repl thread started")
        while !Thread.currentThread().isInterrupted do
          try
            val payload = nodeStatusReceiverService.queue.take()
            val message = new String(payload, "UTF-8")
            val r: Seq[FdHourDigest] = read[Seq[FdHourDigest]](message)
            logger.info(s"Repl received message: $message")
            
          catch
            case _: InterruptedException =>
              Thread.currentThread().interrupt()
            case e: Exception =>
              logger.error("Error in Repl thread", e)
      , "Repl-Receiver")
      t.setDaemon(true)
      t.start()
      thread = Some(t)

  def stop(): Unit =
    thread.foreach(_.interrupt())
    thread = None

  

  def byFdHour: Seq[FdHourDigest] =
    val hourToQsos: Map[FdHour, Seq[Qso]] = qsoStore.all.groupBy(_.fdHour)
    hourToQsos.map { case (fdHour, qsos) =>
      FdHourDigest(fdHour, qsos)
    }.toSeq

  def byFdHourJsonGzip: Array[Byte] =
    val json = write(byFdHour)
    val baos = new ByteArrayOutputStream()
    val gzos = new GZIPOutputStream(baos)
    try
      gzos.write(json.getBytes("UTF-8"))
    finally
      gzos.close()
    baos.toByteArray

  def byFdHourJsonGzipBase64: String =
    Base64.getEncoder.encodeToString(byFdHourJsonGzip)



/**
 * 
 * @param fdHour for when.
 * @param specificQsos what we need. If [[Seq.empty]], all QSOs for the given hour are returned.
 */
case class FdHourRequest(fdHour: FdHour, specificQsos: Seq[Id] = Seq.empty) derives ReadWriter