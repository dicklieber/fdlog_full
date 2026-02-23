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

package fdswarm.client

import cats.effect.IO
import fdswarm.api.PublicApiEndpoints
import fdswarm.fx.bandmodes.BandModeStore.BandModes
import fdswarm.fx.sections.Section
import fdswarm.model.{ApiResponse, BandMode, Qso}
import org.http4s.Uri
import org.http4s.client.Client
import sttp.tapir.client.http4s.Http4sClientInterpreter
import jakarta.inject.{Inject, Singleton}
import com.typesafe.scalalogging.LazyLogging
import cats.effect.unsafe.implicits.global

@Singleton
class FdSwarmRestClient @Inject()(client: Client[IO]) extends LazyLogging:
  // Use SERVER_PORT env var if present, default to 8080
  private val serverPort = sys.env.get("SERVER_PORT").flatMap(_.toIntOption).getOrElse(8080)
  private val baseUri = Uri.unsafeFromString(s"http://localhost:$serverPort")

  private val interpreter = Http4sClientInterpreter[IO]()

  private def handleResponse[T](res: sttp.tapir.DecodeResult[Either[Unit, ApiResponse[T]]]): IO[ApiResponse[T]] =
    res match
      case sttp.tapir.DecodeResult.Value(Right(resp)) => IO.pure(resp)
      case sttp.tapir.DecodeResult.Value(Left(_))      => IO.raiseError(new Exception("API error response"))
      case other                                      => IO.raiseError(new Exception(s"Decode error: $other"))

  def getLastQsos(n: Int): IO[ApiResponse[Seq[Qso]]] =
    val (req, responseParser) = interpreter.toRequest(PublicApiEndpoints.lastQsosDef, Some(baseUri)).apply(n)
    client.run(req).use(responseParser).flatMap(handleResponse)

  def getSections(): IO[ApiResponse[Seq[Section]]] =
    val (req, responseParser) = interpreter.toRequest(PublicApiEndpoints.allSectionsDef, Some(baseUri)).apply(())
    client.run(req).use(responseParser).flatMap(handleResponse)

  def getBandModes(): IO[ApiResponse[BandModes]] =
    val (req, responseParser) = interpreter.toRequest(PublicApiEndpoints.bandModesDef, Some(baseUri)).apply(())
    client.run(req).use(responseParser).flatMap(handleResponse)

  def postQso(qso: Qso): IO[ApiResponse[String]] =
    val (req, responseParser) = interpreter.toRequest(PublicApiEndpoints.postQsoDef, Some(baseUri)).apply(qso)
    client.run(req).use(responseParser).flatMap(handleResponse)

  def getPotentialDups(callsign: String, bandMode: BandMode): IO[ApiResponse[Seq[Qso]]] =
    val (req, responseParser) = interpreter.toRequest(PublicApiEndpoints.potentialDupsDef, Some(baseUri))
      .apply((callsign, bandMode.band, bandMode.mode))
    client.run(req).use(responseParser).flatMap(handleResponse)
