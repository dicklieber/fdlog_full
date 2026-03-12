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
import fdswarm.replication.Transport
import io.micrometer.core.instrument.Gauge
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import jakarta.inject.{Inject, Singleton}
import sttp.tapir.*
import sttp.tapir.server.ServerEndpoint

/**
 * Tapir endpoints for Prometheus metrics.
 */
@Singleton
final class MetricsEndpoints @Inject()(registry: PrometheusMeterRegistry,
                                       transport: Transport) extends ApiEndpoints:

  Gauge.builder("fdswarm_transport_sent_count", transport, (t: Transport) => t.sentCount.toDouble)
    .description("Total number of UDP packets sent by this node")
    .register(registry)

  override def endpoints: List[ServerEndpoint[Any, IO]] = List(metricsEndpoint)

  val metricsEndpoint: ServerEndpoint[Any, IO] =
    MetricsEndpoints.metricsDef
      .serverLogicSuccess[IO] { _ =>
        IO.blocking(registry.scrape())
      }

object MetricsEndpoints:
  val metricsDef: PublicEndpoint[Unit, Unit, String, Any] =
    endpoint
      .get
      .in("metrics")
      .out(stringBody)
      .description("Expose Prometheus metrics")
