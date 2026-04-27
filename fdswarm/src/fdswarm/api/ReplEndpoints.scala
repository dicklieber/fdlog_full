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
import fdswarm.model.Qso
import fdswarm.replication.{LocalNodeStatus, StatusMessage}
import fdswarm.store.QsoStore
import io.circe.generic.semiauto.deriveCodec
import io.circe.syntax.*
import io.circe.{Codec, Printer}
import jakarta.inject.{Inject, Singleton}
import nl.grons.metrics4.scala.DefaultInstrumented
import sttp.tapir.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.ServerEndpoint

import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicLong

/** Tapir endpoints for QSOs. */
@Singleton
final class ReplEndpoints @Inject()(
    qsoStore: QsoStore,
    localNodeStatus: LocalNodeStatus
) extends ApiEndpoints
    with DefaultInstrumented:

  private val allQsoJsonSizeValue = new AtomicLong(0L)
  private val allQsoCount = metrics.counter("allQsoCount")
  metrics.gauge("allQsoJsonSizeBytes")(
    allQsoJsonSizeValue.get()
  )

  override def endpoints: List[ServerEndpoint[Any, IO]] = List(
    allQsos
  )

  val allQsos: ServerEndpoint[Any, IO] =
    ReplEndpoints.allQsosDef
      .serverLogicSuccess[IO] { _ =>
        IO.delay {
          val response = AllQsos(
            statusMessage = localNodeStatus.statusMessage,
            qsos = qsoStore.all
          )
          allQsoCount.inc(
            response.qsos.size.toLong
          )
          val jsonSizeBytes = response.asJson
            .printWith(
              ReplEndpoints.printer
            )
            .getBytes(
              StandardCharsets.UTF_8
            )
            .length
          allQsoJsonSizeValue.set(
            jsonSizeBytes.toLong
          )
          response
        }
      }

object ReplEndpoints:
  val printer: Printer = Printer.spaces2

  val allQsosDef: PublicEndpoint[Unit, Unit, AllQsos, Any] =
    endpoint
      .get
      .in("qsos")
      .out(jsonBody[AllQsos])

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
case class AllQsos(
    statusMessage: StatusMessage,
    qsos: Seq[Qso]
)

object AllQsos:
  given Codec.AsObject[AllQsos] = deriveCodec[AllQsos]
  given Schema[AllQsos] = Schema.derived
