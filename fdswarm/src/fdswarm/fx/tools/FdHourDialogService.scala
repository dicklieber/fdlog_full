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

package fdswarm.fx.tools

import com.typesafe.scalalogging.LazyLogging
import fdswarm.fx.qso.FdHour
import fdswarm.store.QsoStore
import fdswarm.util.HostAndPort
import jakarta.inject.{Inject, Singleton}
import scalafx.Includes.*
import scalafx.geometry.Insets
import scalafx.scene.control.*
import scalafx.scene.layout.FlowPane
import scalafx.stage.Window
import java.net.URI
import java.net.http.{HttpClient, HttpRequest, HttpResponse}
import java.net.http.HttpRequest.BodyPublishers
import java.net.http.HttpResponse.BodyHandlers
import upickle.default.*

@Singleton
final class FdHourDialogService @Inject() (
                                           qsoStore: QsoStore,
                                           networkConfig: fdswarm.replication.NetworkConfig
                                         ) extends LazyLogging:

  private val httpClient = HttpClient.newBuilder()
    .followRedirects(HttpClient.Redirect.NORMAL)
    .build()

  def show(ownerWindow: Window): Unit =
    val digests = qsoStore.digests()
    val apiPort = networkConfig.url.getPort
    val apiHost = networkConfig.url.getHost
    
    val dialog = new Dialog[ButtonType] {
      title = "Send FdHour"
      headerText = s"Select an FdHour to send via HTTP POST to http://$apiHost:$apiPort"
      initOwner(ownerWindow)
    }

    val flowPane = new FlowPane {
      hgap = 10
      vgap = 10
      padding = Insets(10)
      prefWidth = 400
    }

    digests.foreach { digest =>
      val btn = new Button(digest.fdHour.display)
      btn.onAction = _ =>
        sendFdHour(digest.fdHour)
        dialog.result = ButtonType.OK
        dialog.close()
      flowPane.children.add(btn)
    }

    dialog.dialogPane().content = flowPane
    dialog.dialogPane().buttonTypes = Seq(ButtonType.Cancel)

    dialog.showAndWait()

  private def sendFdHour(fdHour: FdHour): Unit =
    val apiPort = networkConfig.url.getPort
    val apiHost = networkConfig.url.getHost
    val url = s"http://$apiHost:$apiPort/hourIds"
    val json = write(Seq(fdHour))
    logger.info(s"Sending FdHour $fdHour to $url via HTTP POST")
    try
      val request = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .header("Content-Type", "application/json")
        .POST(BodyPublishers.ofString(json))
        .build()

      val response = httpClient.send(request, BodyHandlers.ofString())
      
      logger.info(s"Response from $url: ${response.statusCode()}")
      logger.debug(s"Response body: ${response.body()}")
    catch
      case e: Exception =>
        logger.error(s"Failed to send FdHour to $url", e)
