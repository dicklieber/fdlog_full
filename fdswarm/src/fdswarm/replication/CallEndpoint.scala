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
import scala.concurrent.duration.*
import com.typesafe.scalalogging.LazyLogging
import fdswarm.util.HostAndPort
import jakarta.inject.Singleton
import org.http4s.Uri
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.client.middleware.Metrics as ClientMetrics
import _root_.meters4s.Reporter
import _root_.meters4s.http4s.Meters4s
import sttp.tapir.client.http4s.Http4sClientInterpreter

@Singleton
class CallEndpoint @jakarta.inject.Inject()(reporter: Reporter[IO]) extends LazyLogging:

  private val maxRetries = 3
  private val retryDelay = 2.seconds

  /**
   * Calls a remote endpoint with the given input and returns the result.
   * NOTE: We instantiate the Http4sClientInterpreter lazily here to avoid classloading
   * it during test initialization when it's not needed, preventing NoClassDefFoundError
   * if the client dependency isn't on the test runtime classpath.
   */
  def apply[I, E, O]( endpoint: sttp.tapir.PublicEndpoint[I, E, O, Any], input: I)(using hostAndPort: HostAndPort): IO[O] =
    val baseUri = Uri(
      scheme = Some(Uri.Scheme.http),
      authority = Some(Uri.Authority(
        host = Uri.RegName(hostAndPort.host),
        port = Some(hostAndPort.port)
      ))
    )

    val interpreter = Http4sClientInterpreter[IO]()
    val (req, parseResponse) = interpreter.toRequest(endpoint, Some(baseUri)).apply(input)
    
    def executeRequest(attempt: Int): IO[O] =
      logger.debug(s"Calling remote endpoint: ${req.method} ${req.uri} with input: $input (attempt: $attempt)")
      val metricsOps = Meters4s[IO](reporter)
      EmberClientBuilder.default[IO].build.map(ClientMetrics[IO](metricsOps)).use { client =>
        client.run(req).use(parseResponse).flatMap {
          case sttp.tapir.DecodeResult.Value(Right(result: O)) =>
            logger.debug(s"Endpoint call succeeded: ${req.uri} response: $result")
            IO.pure(result)
          case decodeResult => IO.raiseError(new Exception(s"Endpoint call failed: $decodeResult"))
        }
      }.handleErrorWith { error =>
        if attempt < maxRetries then
          logger.warn(s"Remote call to ${req.uri} failed (attempt $attempt): ${error.getMessage}. Retrying in $retryDelay...")
          IO.sleep(retryDelay) >> executeRequest(attempt + 1)
        else
          logger.error(s"Remote call to ${req.uri} failed after $maxRetries attempts: ${error.getMessage}")
          IO.raiseError(error)
      }

    executeRequest(1)
