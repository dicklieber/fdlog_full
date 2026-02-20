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

import cats.effect.IO
import com.typesafe.scalalogging.LazyLogging
import fdswarm.fx.qso.FdHour
import fdswarm.store.{FdHourIds, ReplicationSupport}
import io.circe.syntax.*
import io.circe.Printer
import jakarta.inject.{Inject, Singleton}

import java.net.URI
import java.net.http.HttpResponse.BodyHandlers
import java.net.http.{HttpClient, HttpRequest}

/**
 * Sends replication requests based on incoming status messages.
 *
 * Starts with a [[StatusMessage]], invokes [[ReplicationSupport.determineNeeded]],
 * then makes an HTTP request sending the needed `Seq[FdHour]` as JSON.
 * Additional calls/steps can be added later.
 */
@Singleton
class NeededRequester @Inject()(replicationSupport: ReplicationSupport) extends LazyLogging:

  private val httpClient = HttpClient.newBuilder()
    .followRedirects(HttpClient.Redirect.NORMAL)
    .build()

  private val printer: Printer = Printer.spaces2.copy(dropNullValues = true)

  /**
   * Process an incoming status message: determine what FdHours are needed
   * and POST them to the remote node.
   *
   * @param status incoming status from a remote node
   * @return IO completing after the HTTP call finishes
   */
  def processStatus(status: StatusMessage): IO[Unit] =
    for
      neededIds <- replicationSupport.determineNeeded(status) // IO[Seq[FdHourIds]]
      neededHours = neededIds.map(_.fdHour)                    // Seq[FdHour]
      _ <- sendNeededHours(status, neededHours)
    yield ()

  private def sendNeededHours(status: StatusMessage, hours: Seq[FdHour]): IO[Unit] =
    val url = s"http://${status.hostAndPort}/neededFdHours"
    val json = printer.print(hours.asJson)
    logger.debug(s"POST $url with ${hours.size} FdHour(s)")

    val request = HttpRequest.newBuilder()
      .uri(URI.create(url))
      .header("Content-Type", "application/json")
      .POST(HttpRequest.BodyPublishers.ofString(json))
      .build()

    IO.blocking {
      val response = httpClient.send(request, BodyHandlers.ofString())
      val code = response.statusCode()
      if code == 200 || code == 202 || code == 204 then
        logger.debug(s"NeededRequester: success from $url: $code")
      else
        logger.warn(s"NeededRequester: response from $url: $code body: ${response.body()}")
    }
