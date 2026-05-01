package fdswarm.api

import io.dropwizard.metrics5.*
import munit.FunSuite

import java.util.concurrent.TimeUnit

class PrometheusMetricsTest extends FunSuite:

  test("renders counters meters histograms timers and numeric gauges"):
    val registry = new MetricRegistry()

    val counter = registry.counter(
      "tcp.requests"
    )
    counter.inc(
      3
    )

    val histogram = registry.histogram(
      "tcp.payload.bytes"
    )
    histogram.update(
      12
    )

    val timer = registry.timer(
      "tcp.latency"
    )
    timer.update(
      250,
      TimeUnit.MILLISECONDS
    )

    registry.registerGauge(
      "tcp.current.nodes",
      new Gauge[Int]:
        override def getValue: Int = 4
    )

    val output = PrometheusMetrics.render(
      registry
    )

    assert(
      output.contains(
        "# TYPE fdswarm_tcp_requests_total counter"
      )
    )
    assert(
      output.contains(
        "fdswarm_tcp_requests_total 3.0"
      )
    )
    assert(
      output.contains(
        "# TYPE fdswarm_tcp_payload_bytes summary"
      )
    )
    assert(
      output.contains(
        """fdswarm_tcp_payload_bytes{quantile="0.95"}"""
      )
    )
    assert(
      output.contains(
        "fdswarm_tcp_payload_bytes_count 1.0"
      )
    )
    assert(
      output.contains(
        "# TYPE fdswarm_tcp_latency_seconds summary"
      )
    )
    assert(
      output.contains(
        "fdswarm_tcp_latency_seconds_count 1.0"
      )
    )
    assert(
      output.contains(
        "# TYPE fdswarm_tcp_current_nodes gauge"
      )
    )
    assert(
      output.contains(
        "fdswarm_tcp_current_nodes 4.0"
      )
    )

  test("skips non-numeric gauges"):
    val registry = new MetricRegistry()
    registry.registerGauge(
      "tcp.status",
      new Gauge[String]:
        override def getValue: String = "up"
    )

    val output = PrometheusMetrics.render(
      registry
    )

    assertEquals(
      output,
      ""
    )
