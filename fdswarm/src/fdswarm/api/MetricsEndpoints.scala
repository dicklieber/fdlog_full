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
import com.google.inject.Inject
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import sttp.tapir.*
import sttp.tapir.CodecFormat
import sttp.tapir.server.ServerEndpoint


class MetricsEndpoints @Inject()(registry: PrometheusMeterRegistry) extends ApiEndpoints:
  override def endpoints: List[ServerEndpoint[Any, IO]] = List(metrics)
  val metrics: ServerEndpoint[Any, IO] =
    endpoint
      .get
      .in("metrics")
      .out(stringBody)
      .serverLogicSuccess[IO] { _ =>
        val metricsData = registry.scrape()
        IO.pure(metricsData)
      }
