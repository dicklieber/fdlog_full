package fdswarm.metric

import io.dropwizard.metrics5.*
import munit.FunSuite

class MetricSnapshotFactoryTest extends FunSuite:

  test("creates counter snapshot"):
    val counter = new Counter()
    counter.inc(
      3
    )

    assertEquals(
      MetricSnapshotFactory(
        counter
      ),
      CounterSnapshot(
        count = 3L
      )
    )

  test("creates gauge snapshot"):
    val gauge = new Gauge[Int]:
      override def getValue: Int = 42

    assertEquals(
      MetricSnapshotFactory(
        gauge
      ),
      GaugeSnapshot(
        value = "42"
      )
    )


  test("creates histogram snapshot"):
    val histogram = new Histogram(
      new UniformReservoir()
    )
    histogram.update(
      7
    )

    val snapshot = MetricSnapshotFactory(
      histogram
    ).asInstanceOf[HistogramSnapshot]

    assertEquals(
      snapshot.count,
      1L
    )
    assertEquals(
      snapshot.min,
      7L
    )
    assertEquals(
      snapshot.max,
      7L
    )
    assertEquals(
      snapshot.metricType,
      MetricType.Histogram
    )

  test("creates timer snapshot"):
    val timer = new Timer()
    timer.update(
      100,
      java.util.concurrent.TimeUnit.NANOSECONDS
    )

    val snapshot = MetricSnapshotFactory(
      timer
    ).asInstanceOf[TimerSnapshot]

    assertEquals(
      snapshot.count,
      1L
    )
    assertEquals(
      snapshot.metricType,
      MetricType.Timer
    )

  test("returns none for unsupported metric"):
    val metric = new Metric {}

    assertEquals(
      MetricSnapshotFactory.fromMetric(
        metric
      ),
      None
    )
