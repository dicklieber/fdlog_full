package fdswarm.telemetry

import io.prometheus.metrics.expositionformats.ExpositionFormats
import com.codahale.metrics.{Counter, Gauge, MetricRegistry, Timer}
import com.codahale.metrics.SharedMetricRegistries
import io.prometheus.metrics.expositionformats.PrometheusTextFormatWriter
import io.prometheus.metrics.instrumentation.dropwizard.DropwizardExports
import io.prometheus.metrics.model.registry.PrometheusRegistry
import jakarta.inject.*

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

@Singleton
final class Metrics @Inject()():
  // metrics4-scala DefaultInstrumented publishes into SharedMetricRegistries "default"
  private val registry: MetricRegistry = SharedMetricRegistries.getOrCreate(
    "default"
  )

  private val prometheusRegistry = new PrometheusRegistry()
  private val prometheusTextFormatWriter = ExpositionFormats
    .init()
    .getPrometheusTextFormatWriter()
  prometheusRegistry.register(
    new DropwizardExports(
      registry
    )
  )


  def prometheusScrape(): String =
    prometheusTextFormatWriter.toDebugString(
      prometheusRegistry.scrape()
    )

  def prometheusContentType: String =
    PrometheusTextFormatWriter.CONTENT_TYPE

  /** Compatibility alias for old call sites. */
  def scrape(): String =
    prometheusScrape()

  def registryRef: MetricRegistry =
    registry
