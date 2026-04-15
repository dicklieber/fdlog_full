package fdswarm.logging

import com.google.inject.name.Named
import fdswarm.logging.Locus.Metrics
import jakarta.inject.{Inject, Singleton}
import nl.grons.metrics4.scala.DefaultInstrumented

import java.time.Duration

/**
 * Handles the shipping of NDJSON (Newline Delimited JSON) data to an Elasticsearch Bulk API endpoint.
 *
 * This class provides functionality for sending NDJSON payloads to Elasticsearch via the bulk endpoint.
 * API key credentials can be configured through environment variables or application configuration.
 *
 * Metrics related to the shipping process are captured using OpenTelemetry.
 *
 * @constructor
 * Creates an instance of ElasticShipper using the following parameters:
 * @param endpoint         The Elasticsearch Bulk API endpoint.
 * @param configuredApiKey The API key configured through the application setting.
 * @param connectTimeout   The connection timeout for the HTTP client.
 * @param requestTimeout   The timeout for Elasticsearch requests.
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
  requestTimeout: Duration
) extends DefaultInstrumented with LazyStructuredLogging(Metrics):

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
    shipCount.inc()
    shipTime.time(
      elasticBulkLogger.sendNdjson(
        ndJson
      ))


  private def sendJsonEvent(
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

  private val shipTime = metrics.timer("es_ship_duration")
  private val shipCount = metrics.counter("es_ship_count")