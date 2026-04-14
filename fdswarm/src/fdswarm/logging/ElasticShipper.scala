package fdswarm.logging

import com.google.inject.name.Named
import io.micrometer.core.instrument.MeterRegistry
import jakarta.inject.{Inject, Singleton}

import java.time.Duration

/**
 * Handles the shipping of NDJSON (Newline Delimited JSON) data to an Elasticsearch Bulk API endpoint.
 *
 * This class provides functionality for sending NDJSON payloads to Elasticsearch via the bulk endpoint.
 * API key credentials can be configured through environment variables or application configuration.
 *
 * Metrics related to the shipping process are captured using a MeterRegistry instance.
 *
 * @constructor
 * Creates an instance of ElasticShipper using the following parameters:
 * @param endpoint         The Elasticsearch Bulk API endpoint.
 * @param configuredApiKey The API key configured through the application setting.
 * @param connectTimeout   The connection timeout for the HTTP client.
 * @param requestTimeout   The timeout for Elasticsearch requests.
 * @param meterRegistry    The metrics registry for capturing shipping metrics.
 *
 *                         This class incorporates logging through LazyStructuredLogging. If the API key is overridden by
 *                         an environment variable, the class logs this behavior.
 *
 *                         The key functionalities include:
 *                         - Sending NDJSON data to Elasticsearch.
 *                         - Incrementing the send operation counter for observability.
 */
@Singleton
final class ElasticShipper @Inject()(
  @Named("fdswarm.elastic.endpoint")
  endpoint: String,
  @Named("fdswarm.elastic.apiKey")
  configuredApiKey: String,
  @Named("fdswarm.elastic.connectTimeout")
  connectTimeout: Duration,
  @Named("fdswarm.elastic.requestTimeout")
  requestTimeout: Duration,
  meterRegistry: MeterRegistry
) extends LazyStructuredLogging:

  private val apiKeyEnvVarName = "ES_API_KEY"
  private val apiKey =
    Option(
      System.getenv(
        apiKeyEnvVarName
      )
    ).filter(
      _.nonEmpty
    ).getOrElse(
      configuredApiKey
    )

  if apiKey != configuredApiKey then
    logger.info(
      s"ElasticShipper apiKey overridden by environment variable $apiKeyEnvVarName"
    )

  private val sendCounter =
    meterRegistry.counter(
      "fdswarm_elastic_shipper_send_total"
    )

  private val sendTimer =
    meterRegistry.timer(
      "fdswarm_elastic_shipper_send_duration"
    )


  private val elasticBulkLogger =
    new ElasticBulkLogger(
      endpoint = endpoint,
      apiKey = apiKey,
      connectTimeout = connectTimeout,
      requestTimeout = requestTimeout
    )

  StructuredLogger.setJsonEventSink(
    eventJson =>
      sendJsonEvent(
        eventJson
      )
  )

  def send(
    ndJson: String
  ): Unit =
    sendCounter.increment()
    sendTimer.record(
      () =>
        elasticBulkLogger.sendNdjson(
          ndJson
        )
    )

  def sendJsonEvent(
    eventJson: String,
    index: String = "fdswarm-logs"
  ): Unit =
    require(
      eventJson != null && eventJson.trim.nonEmpty,
      "eventJson must not be null or empty"
    )
    require(
      index != null && index.trim.nonEmpty,
      "index must not be null or empty"
    )

    val metadataLine =
      s"""{"index":{"_index":"$index"}}"""
    val payload =
      s"$metadataLine\n${eventJson.trim}\n"

    send(
      payload
    )
