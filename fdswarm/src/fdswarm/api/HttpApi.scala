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

import cats.effect.{IO, Resource}
import cats.effect.unsafe.implicits.global
import com.typesafe.scalalogging.LazyLogging
import fdswarm.util.HostAndPortProvider
import jakarta.inject.{Inject, Singleton}
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Router
import org.http4s.server.middleware.Logger as Http4sLogger
import org.http4s.HttpApp
import sttp.tapir.server.http4s.Http4sServerInterpreter
import com.comcast.ip4s.{Host, Port}

import scala.jdk.CollectionConverters.*

/**
 *
 * @param apiEndpoints as collected by [[AutoBind]] in [[ConfigModule]].
 * @param hostAndPortProvider our ports and hostnames.
 */
@Singleton
final class HttpApi @Inject()(apiEndpoints: java.util.Set[ApiEndpoints],
                              hostAndPortProvider: HostAndPortProvider)
  extends LazyLogging:

  /** Starts the HTTP server on a daemon thread. */
  def start(): Unit =
    val port = Port.fromInt(hostAndPortProvider.http.port).get
    val host = Host.fromString("0.0.0.0").get

    val serverResource: Resource[IO, org.http4s.server.Server] =
      EmberServerBuilder.default[IO]
        .withHost(host)
        .withPort(port)
        .withHttpApp(httpApp)
        .build

    val t = new Thread(() =>
      // keep running until JVM exits
      try
        serverResource.useForever.unsafeRunSync()
      catch
        case e: Throwable =>
          logger.error("HTTP API server failed", e)
    )
    t.setName("fdlog-http-api")
    t.setDaemon(true)
    t.start()

  private def httpApp: HttpApp[IO] =
    val endpoints = apiEndpoints.asScala.flatMap(_.endpoints).toList
    val routes = Http4sServerInterpreter[IO]().toRoutes(endpoints)
    val app = Router("/" -> routes).orNotFound
    // Log requests/responses at info level
    Http4sLogger.httpApp(logHeaders = true, logBody = false)(app)
