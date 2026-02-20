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
import fdswarm.util.Ids.Id
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

  private val printer: Printer = Printer.spaces2.copy(dropNullValues = true)

  override def endpoints: List[ServerEndpoint[Any, IO]] = List( qsosForIds, qsoIdsForFdHour, neededFdHours)



  val qsoIdsForFdHour: ServerEndpoint[Any, IO] =
    ReplEndpoints.qsoIdsByHourGetDef
      .serverLogicSuccess[IO] { fdHour =>
        replicationSupport.idsForHour(fdHour).map(_.ids)
      }

  val qsosForIds: ServerEndpoint[Any, IO] =
    ReplEndpoints.qsosForIdsDef
      .serverLogicSuccess[IO] { request =>
        replicationSupport.qsosForIds(request).map(_.qsos)
      }

  val neededFdHours: ServerEndpoint[Any, IO] =
    ReplEndpoints.neededFdHoursDef
      .serverLogicSuccess[IO] { hours =>
        // This endpoint should return the FdHourIds for each requested hour
        import cats.implicits.*
        hours.traverse(replicationSupport.idsForHour).map(_.toList)
      }

/**
 * vals here ending in "Def" are tapir endpoints.
 * The are used ServerEndpoint.serverLogicSuccess[IO] to provide the actual logic. above.
 * and in client code
 */
object ReplEndpoints:
//  val allQsosDef =
//    endpoint
//      .get
//      .in("qsos")
//      .out(jsonBody[Seq[Qso]])
//      .out(header[String]("Content-Type"))
//      .out(header[String]("Content-Disposition"))

  
  val qsoIdsByHourGetDef =
    endpoint
      .get
      .in("qsos" / "ids" / path[FdHour])
      .out(jsonBody[Seq[Id]])

  val qsosForIdsDef =
    endpoint
      .post
      .in("qsosForIds")
      .in(jsonBody[FdHourRequest])
      .out(jsonBody[Seq[Qso]])

  val neededFdHoursDef =
    endpoint
      .post
      .in("neededFdHours")
      .in(jsonBody[Seq[FdHour]])
      .out(jsonBody[List[FdHourIds]])
