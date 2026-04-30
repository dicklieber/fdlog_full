package fdswarm.telemetry

import jakarta.inject.*

@Singleton
final class Metrics @Inject()():
  // metrics4-scala DefaultInstrumented publishes into SharedMetricRegistries "default"
  val registry: MetricRegistry = SharedMetricRegistries.getOrCreate(
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
