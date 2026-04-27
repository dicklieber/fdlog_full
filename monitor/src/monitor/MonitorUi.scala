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
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.Scene
import scalafx.scene.control.Label
import scalafx.scene.layout.StackPane
import scalafx.scene.text.Font
import scalafx.stage.Stage

import java.util.concurrent.LinkedBlockingQueue

final class MonitorUi @Inject() (udpPacketListener: UdpPacketListener) extends LazyStructuredLogging:

  def start(primaryStage: Stage): Unit =
    primaryStage.title = "Monitor"
    primaryStage.scene = new Scene:
      root = new StackPane:
        padding = Insets(24)
        alignment = Pos.Center
        children = Seq(
          new Label("hello monitor"):
            font = Font.font(28)
        )

  private val queue: LinkedBlockingQueue[Packet] = udpPacketListener.incomingQueue
  while true
  do {
    val packet = queue.take()
    logger.info( "packet"-> packet.toString)
  }