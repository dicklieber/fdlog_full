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
import com.organization.BuildInfo
import fdswarm.fx.bandmodes.BandModeStore
import fdswarm.fx.bandmodes.BandModeStore.BandModes
import fdswarm.fx.sections.{Section, SectionsProvider}
import fdswarm.model.{ApiResponse, NodeHeader, Qso}
import fdswarm.replication.UDPHeader
import fdswarm.store.QsoStore
import fdswarm.util.HostAndPortProvider
import jakarta.inject.{Inject, Singleton}
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.ServerEndpoint

import java.time.Instant

/** Tapir endpoint definitions for the public API. */
object PublicApiEndpoints {
  private val baseEndpoint = endpoint.in("api")

  val lastQsosDef = baseEndpoint.get
    .in("qsos" / "last" / path[Int]("n"))
    .out(jsonBody[ApiResponse[Seq[Qso]]])
    .description("Gets the last n QSOs")

  val allSectionsDef = baseEndpoint.get
    .in("sections")
    .out(jsonBody[ApiResponse[Seq[Section]]])
    .description("Gets all sections")

  val bandModesDef = baseEndpoint.get
    .in("bandmodes")
    .out(jsonBody[ApiResponse[BandModes]])
    .description("Gets BandModes")
}

/** Implementation of the public API endpoints. */
@Singleton
class PublicApiRoutes @Inject()(
    qsoStore: QsoStore,
    sectionsProvider: SectionsProvider,
    bandModeStore: BandModeStore,
    hostAndPortProvider: HostAndPortProvider
) extends ApiEndpoints {

  override def endpoints: List[ServerEndpoint[Any, IO]] = List(
    PublicApiEndpoints.lastQsosDef.serverLogicSuccess(n => IO.pure(withHeader(qsoStore.all.takeRight(n)))),
    PublicApiEndpoints.allSectionsDef.serverLogicSuccess(_ => IO.pure(withHeader(sectionsProvider.allSections))),
    PublicApiEndpoints.bandModesDef.serverLogicSuccess(_ => IO.pure(withHeader(bandModeStore.currentBandMode)))
  )

  private def withHeader[T](data: T): ApiResponse[T] = {
    val header = NodeHeader(
      version = BuildInfo.version,
      hostAndPort = hostAndPortProvider.http,
      udpInstanceId = UDPHeader.localInstanceId,
      timestamp = Instant.now()
    )
    ApiResponse(header, data)
  }
}
