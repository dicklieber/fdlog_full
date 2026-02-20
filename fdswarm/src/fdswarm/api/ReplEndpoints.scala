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
import fdswarm.store.{FdHourIds, FdHourQsos, FdHourRequest, QsoStore, ReplicationSupport}
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
final class ReplEndpoints @Inject()(replicationSupport: ReplicationSupport,
                                    registry: PrometheusMeterRegistry) extends ApiEndpoints:

  override def endpoints: List[ServerEndpoint[Any, IO]] = List(allQsos, qsoIdsByHour, qsosForIds, qsoIdsByHourPost, neededFdHours)

  private val printer: Printer = Printer.spaces2.copy(dropNullValues = true)

  val allQsos: ServerEndpoint[Any, IO] =
    ReplEndpoints.allQsosDef
      .serverLogicSuccess[IO] { _ =>
        IO.pure((replicationSupport.all, "application/json", "attachment; filename=qsos.json"))
      }

  val qsoIdsByHour: ServerEndpoint[Any, IO] =
    ReplEndpoints.qsoIdsByHourDef
      .serverLogicSuccess[IO] { fdHourStr =>
        val fdHour = FdHour(fdHourStr)
        replicationSupport.idsForHour(fdHour)
      }

  val qsoIdsByHourPost: ServerEndpoint[Any, IO] =
    ReplEndpoints.qsoIdsByHourPostDef
      .serverLogicSuccess[IO] { fdHour =>
        replicationSupport.idsForHour(fdHour)
      }

  val qsosForIds: ServerEndpoint[Any, IO] =
    ReplEndpoints.qsosForIdsDef
      .serverLogicSuccess[IO] { request =>
        replicationSupport.qsosForIds(request)
      }

  val neededFdHours: ServerEndpoint[Any, IO] =
    ReplEndpoints.neededFdHoursDef
      .serverLogicSuccess[IO] { hours =>
        // This endpoint should return the FdHourIds for each requested hour
        import cats.implicits.*
        hours.traverse(replicationSupport.idsForHour).map(_.toList)
      }

object ReplEndpoints:
  val allQsosDef =
    endpoint
      .get
      .in("qsos")
      .out(jsonBody[Seq[Qso]])
      .out(header[String]("Content-Type"))
      .out(header[String]("Content-Disposition"))

  val qsoIdsByHourDef =
    endpoint
      .get
      .in("qsos" / path[String]("fdHour"))
      .out(jsonBody[FdHourIds])

  val qsoIdsByHourPostDef =
    endpoint
      .post
      .in("qsos" / "ids")
      .in(jsonBody[FdHour])
      .out(jsonBody[FdHourIds])

  val qsosForIdsDef =
    endpoint
      .post
      .in("qsosForIds")
      .in(jsonBody[FdHourRequest])
      .out(jsonBody[FdHourQsos])

  val neededFdHoursDef =
    endpoint
      .post
      .in("neededFdHours")
      .in(jsonBody[Seq[FdHour]])
      .out(jsonBody[List[FdHourIds]])
