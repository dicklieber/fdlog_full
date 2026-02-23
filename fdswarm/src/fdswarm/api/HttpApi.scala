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
import org.http4s.server.middleware.{Logger as Http4sLogger, Metrics as ServerMetrics}
import org.http4s.headers.{Referer, `User-Agent`}
import org.http4s.{Request, Response}
import java.time.format.DateTimeFormatter
import java.time.ZonedDateTime
import java.util.Locale
import org.slf4j.LoggerFactory
import org.http4s.HttpApp
import org.http4s.syntax.all.*
import cats.syntax.all.*
import _root_.meters4s.Reporter
import _root_.meters4s.http4s.Meters4s
import sttp.tapir.server.http4s.Http4sServerInterpreter
import com.comcast.ip4s.{Host, Port}
import sttp.tapir.swagger.bundle.SwaggerInterpreter

import scala.jdk.CollectionConverters.*

/**
 *
 * @param apiEndpoints        as collected by [[AutoBind]] in [[ConfigModule]].
 * @param hostAndPortProvider our ports and hostnames.
 */
@Singleton
final class HttpApi @Inject()(apiEndpoints: java.util.Set[ApiEndpoints],
                              hostAndPortProvider: HostAndPortProvider,
                              reporter: Reporter[IO])
  extends LazyLogging:

  private def accessLogger = LoggerFactory.getLogger("org.http4s.server.middleware.Logger")
  private val clfTimeFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss Z", Locale.ENGLISH)

  /** Wraps an HttpApp and logs each request in Common Log Format to `access.log`. */
  private def commonLogFormat(app: HttpApp[IO]): HttpApp[IO] = HttpApp[IO] { req =>
    app(req).flatTap { resp =>
      val host = req.remote.map(_.host.toString).getOrElse("-")
      val ident = "-"
      val user = "-"
      val ts = ZonedDateTime.now().format(clfTimeFmt)

      val path =
        val p = req.uri.path.renderString
        val q = req.uri.query.renderString
        if q.nonEmpty then s"$p?$q" else p

      if path.startsWith("/metrics") then
        IO.unit
      else
        val reqLine = s"${req.method.name} $path ${req.httpVersion}"
        val status = resp.status.code
        val bytes = resp.contentLength.getOrElse(0L)

        IO(accessLogger.info(s"""$host $ident $user [$ts] "$reqLine" $status $bytes"""))
    }
  }

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
    val swaggerEndpoints = SwaggerInterpreter().fromServerEndpoints[IO](endpoints, "Field Day Swarm API", "1.0.0")

    val routes = Http4sServerInterpreter[IO]().toRoutes(endpoints)
    val swaggerRoutes = Http4sServerInterpreter[IO]().toRoutes(swaggerEndpoints)

    val metricsOps = Meters4s[IO](reporter)
    val instrumentedRoutes = ServerMetrics[IO](metricsOps)(routes <+> swaggerRoutes)

    val app = Router("/" -> instrumentedRoutes).orNotFound: HttpApp[IO]
    // Log each request in Common Log Format (CLF) to the dedicated access logger
    commonLogFormat(app)
