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

package fdswarm.api

import cats.effect.IO
import fdswarm.model.{Callsign, Qso}
import fdswarm.store.{FdHourIds, FdHourQsos, FdHourRequest, QsoStore}
import io.circe.syntax.*
import io.circe.Printer
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import jakarta.inject.Inject
import sttp.tapir.*
import sttp.tapir.CodecFormat
import sttp.tapir.server.ServerEndpoint
import fdswarm.fx.qso.FdHour
import sttp.tapir.json.circe.*
import sttp.tapir.generic.auto.*

import java.time.Instant

/** Tapir endpoints for QSOs. */
final class ReplEndpoints @Inject()(qsoStore: QsoStore,
                                    registry: PrometheusMeterRegistry) extends ApiEndpoints:

  override def endpoints: List[ServerEndpoint[Any, IO]] = List(allQsos, qsoIdsByHour, qsosForIds)

  /**
    * GET /qsos – returns all QSOs as JSON and sets headers to download as an attachment.
    * Equivalent to the cask route:
    *   @get("/qsos"): Response[String](json, headers = ["Content-Type", "Content-Disposition"]) 
    */
  private val printer: Printer = Printer.spaces2.copy(dropNullValues = true)

  val allQsos: ServerEndpoint[Any, IO] =
    endpoint
      .get
      .in("qsos")
      .out(stringBody)
      .out(header[String]("Content-Type"))
      .out(header[String]("Content-Disposition"))
      .serverLogicSuccess[IO] { _ =>
        val json = printer.print(qsoStore.all.asJson)
        IO.pure((json, "application/json", "attachment; filename=qsos.json"))
      }

  /**
    * GET /qsos/{fdHour}/ids – returns all UUIDs for the given FdHour.
    */
  val qsoIdsByHour: ServerEndpoint[Any, IO] =
    endpoint
      .get
      .in("qsos" / path[String]("fdHour"))
      .out(stringBody)
      .out(header[String]("Content-Type"))
      .serverLogicSuccess[IO] { fdHourStr =>
        val fdHour = FdHour(fdHourStr)
        val result: FdHourIds = qsoStore.idsForHour(fdHour)
        val json = printer.print(result.asJson)
        IO.pure((json, "application/json"))
      }

  /**
    * POST /qsosForIds – returns QSOs for the given IDs in an FdHourRequest.
    */
  val qsosForIds: ServerEndpoint[Any, IO] =
    endpoint
      .post
      .in("qsosForIds")
      .in(stringBody) 
      .out(stringBody)
      .out(header[String]("Content-Type"))
      .serverLogicSuccess[IO] { requestJson =>
        import io.circe.parser.decode
        decode[FdHourRequest](requestJson) match {
          case Right(request) =>
            val result = qsoStore.qsosForIds(request)
            IO.pure((printer.print(result.asJson), "application/json"))
          case Left(error) =>
            IO.raiseError(new Exception(s"Failed to decode FdHourRequest: ${error.getMessage}"))
        }
      }
