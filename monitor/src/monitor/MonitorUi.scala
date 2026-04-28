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

package monitor

import com.google.inject.Inject
import fdswarm.logging.LazyStructuredLogging
import fdswarm.replication.UDPHeaderData
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.Scene
import scalafx.scene.control.Label
import scalafx.scene.layout.StackPane
import scalafx.scene.text.Font
import scalafx.stage.Stage

import java.util.concurrent.LinkedBlockingQueue
import scala.util.control.NonFatal

final class MonitorUi @Inject() (udpPacketListener: UdpPacketListener)
    extends LazyStructuredLogging:
  private val queue: LinkedBlockingQueue[UDPHeaderData] = udpPacketListener.incomingQueue
  private val packetLoggerThread = new Thread(
    () => consumePackets(),
    "Monitor-Packet-Logger"
  )
  @volatile private var stopped = false
  packetLoggerThread.setDaemon(true)

  def start(primaryStage: Stage): Unit =
    primaryStage.title = "Monitor"
    primaryStage.onCloseRequest = _ => stop()
    primaryStage.scene = new Scene:
      root = new StackPane:
        padding = Insets(24)
        alignment = Pos.Center
        children = Seq(
          new Label("hello monitor"):
            font = Font.font(28)
        )

    packetLoggerThread.start()

  private def stop(): Unit =
    stopped = true
    packetLoggerThread.interrupt()
    udpPacketListener.stop()

  private def consumePackets(): Unit =
    while !stopped && !Thread.currentThread().isInterrupted do
      try
        val uDPHeaderData: UDPHeaderData = queue.take()
        logger.info(uDPHeaderData.toString)
      catch
        case _: InterruptedException =>
          Thread.currentThread().interrupt()
        case NonFatal(e) =>
          logger.error("Error handling monitor packet", e)
