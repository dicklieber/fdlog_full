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
import fdswarm.store.QsoStore
import io.circe.Printer
import jakarta.inject.Inject
import sttp.tapir.server.ServerEndpoint

/** Tapir endpoints for QSOs. */
final class ReplEndpoints @Inject()(
  qsoStore: QsoStore
) extends ApiEndpoints:

  private val printer: Printer = Printer.spaces2.copy(dropNullValues = true)

  override def endpoints: List[ServerEndpoint[Any, IO]] = List(
//    qsosForIds,
//    qsoIdsForFdHour,
//    neededFdHours
  )


/**
 * vals here ending in "Def" are tapir endpoints.
 * The are used ServerEndpoint.serverLogicSuccess[IO] to provide the actual logic. above.
 * and in client code
 */
//object ReplEndpoints:
//  val allQsosDef =
//    endpoint
//      .get
//      .in("qsos")
//      .out(jsonBody[Seq[Qso]])
//      .out(header[String]("Content-Type"))
//      .out(header[String]("Content-Disposition"))

//
//  val qsoIdsByHourGetDef =
//    endpoint
//      .get
//      .in("qsos" / "ids" / path[FdHour])
//      .out(jsonBody[Seq[Id]])
//
//  val qsosForIdsDef =
//    endpoint
//      .post
//      .in("qsosForIds")
//      .in(jsonBody[FdHourRequest])
//      .out(jsonBody[Seq[Qso]])
//
//  val neededFdHoursDef =
//    endpoint
//      .post
//      .in("neededFdHours")
//      .in(jsonBody[Seq[FdHour]])
//      .out(jsonBody[List[FdHourIds]])
