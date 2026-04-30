package fdswarm.util

import fdswarm.logging.Locus
import io.dropwizard.metrics5.*

import scala.collection.mutable

trait StatsSource(locus:Locus):
  private val metricRegistry: MetricRegistry = new MetricRegistry()
  private val ourMetrics: mutable.Set[Metric] = mutable.Set[Metric]()
  private def prefixWithLocus(name: String): String = s"${locus.value}.$name"

  /**
   * Tracks and registers the given metric in the internal set of metrics.
   *
   * @param metric the metric instance to be registered and tracked.
   * @return the same metric instance that was passed as an argument. */
  private def track[T <: Metric](metric: T): T =
    ourMetrics += metric
    metric

  def addCounter(name: String): Counter =
    track(metricRegistry.counter(prefixWithLocus(name)))

  def addHistogram(name: String): Histogram =
    track(metricRegistry.histogram(prefixWithLocus(name)))

  def addMeter(name: String): Meter =
    track(metricRegistry.meter(prefixWithLocus(name)))

  def addTimer(name: String): Timer =
    track(metricRegistry.timer(prefixWithLocus(name)))

  private def addGauge[T](name: String, gauge: Gauge[T]): Gauge[T] =
    track(metricRegistry.registerGauge(name, gauge))

  def addGauge[T](name: String)(value: => T): Gauge[T] =
    addGauge(
      name,
      new Gauge[T]:
        override def getValue: T = value
    )

//  def addSettableGauge[T](name: String): SettableGauge[T] =
//    val gauge = new DefaultSettableGauge[T]()
//    metricRegistry.registerGauge(name, gauge)
//    track(gauge)

//  def addSettableGauge[T](name: String, initialValue: T): SettableGauge[T] =
//    val gauge = new DefaultSettableGauge[T](initialValue)
//    metricRegistry.registerGauge(name, gauge)
//    track(gauge)

//  def addMetric[T <: Metric](name: String, metric: T): T =
//    addMetric(MetricRegistry.name(prefixWithLocus(name)), metric)
//
//  def addMetric[T <: Metric](name: MetricName, metric: T): T =
//    track(metricRegistry.register(name, metric))

//  def addMetricSet(metricSet: MetricSet): MetricSet =
//    metricRegistry.registerAll(metricSet)
//    track(metricSet)
//
//  def addMetricSet(name: String, metricSet: MetricSet): MetricSet =
//    metricRegistry.registerAll(MetricRegistry.name(prefixWithLocus(name)), metricSet)
//    track(metricSet)
