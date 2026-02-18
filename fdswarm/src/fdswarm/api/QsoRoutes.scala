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

import cask.*
import com.google.inject.Inject
import com.typesafe.scalalogging.LazyLogging
import fdswarm.fx.qso.FdHour
import fdswarm.fx.qso.FdHour.given
import fdswarm.store.QsoStore
import fdswarm.util.UPickleGzip
import upickle.default.*

class QsoRoutes @Inject()(qsoStore: QsoStore) extends Routes with LazyLogging:
  override def decorators = Seq(new cask.decorators.compress())
  @get("/qsos")
  def allQsos(): Response[String] =
    Response(
      data = write(qsoStore.all, indent = 2),
      headers = Seq(
        "Content-Type" -> "application/json",
        "Content-Disposition" -> "attachment; filename=qsos.json"
      )
    )
  

  @get("/hourQsos/:fdHour")
  def hourQsos(fdHour: FdHour): Response[String] =
    val forHour = qsoStore.qsosForFdHour(fdHour)
    val json = write(forHour)
    logger.debug(s"/hourQsos/:fdHour ${json.length} bytes of JSON")
    Response(
      data = json,
      statusCode = 200,
      headers = Seq("Content-Type" -> "application/json")
    )


  initialize()

import cask.endpoints.QueryParamReader
import cask.model.Request

//  given QueryParamReader[FdHour] with
//    def arity: Int = 1
//
//    def read(ctx: Request, label: String, v: Seq[String]): FdHour =
//      v match
//        case Seq(s) => FdHour(s)
//        case Nil =>
//          throw new IllegalArgumentException(s"Missing parameter '$label'")
//        case xs =>
//          throw new IllegalArgumentException(
//            s"Expected 1 value for '$label', got ${xs.size}"
//          )