package fdswarm.telemetry

import cats.effect.IO
import jakarta.inject.*
import sttp.tapir.*
import sttp.tapir.server.ServerEndpoint

@Singleton
final class MetricsEndpoints @Inject()(metrics: Metrics):

  val metricsEndpoint: ServerEndpoint[Any, IO] =
    endpoint.get
      .in("metrics")
      .out(
        header[String]("Content-Type")
          .and(stringBody)
      )
      .serverLogicSuccess[IO] { _ =>
        IO.pure("text/plain; version=0.0.4; charset=utf-8" -> metrics.prometheusScrape())
      }