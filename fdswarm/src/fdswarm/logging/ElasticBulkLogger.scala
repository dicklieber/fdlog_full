package fdswarm.logging

import java.net.URI
import java.net.http.{HttpClient, HttpRequest, HttpResponse}
import java.time.Duration
import java.util.Objects

/** Sends NDJSON batches to Elasticsearch Bulk API using an API key.
 *
 * Expected endpoint example:
 *   http://localhost:9200/_bulk
 *
 * Expected auth header format:
 *   Authorization: ApiKey <base64(id:api_key)>
 *
 * The ndjson string must already be in Elasticsearch bulk format and must end with a newline.
 */
final class ElasticBulkLogger(
                               endpoint: String,
                               apiKey: String,
                               connectTimeout: Duration = Duration.ofSeconds(5),
                               requestTimeout: Duration = Duration.ofSeconds(15)
                             ):

  Objects.requireNonNull(endpoint, "endpoint must not be null")
  Objects.requireNonNull(apiKey, "apiKey must not be null")
  Objects.requireNonNull(connectTimeout, "connectTimeout must not be null")
  Objects.requireNonNull(requestTimeout, "requestTimeout must not be null")

  require(endpoint.nonEmpty, "endpoint must not be empty")
  require(apiKey.nonEmpty, "apiKey must not be empty")

  private val client: HttpClient =
    HttpClient.newBuilder()
      .connectTimeout(connectTimeout)
      .build()

  private val authHeaderValue = s"ApiKey $apiKey"

  /** Sends one already-built NDJSON bulk request.
   *
   * Returns the raw response body on success.
   * Throws an exception on HTTP or transport failure.
   */
  def sendNdjson(ndjson: String): String =
    validateNdjson(ndjson)

    val request =
      HttpRequest.newBuilder()
        .uri(URI.create(endpoint))
        .timeout(requestTimeout)
        .header("Authorization", authHeaderValue)
        .header("Content-Type", "application/x-ndjson")
        .POST(HttpRequest.BodyPublishers.ofString(ndjson))
        .build()

    val response =
      client.send(request, HttpResponse.BodyHandlers.ofString())

    val status = response.statusCode()
    val body   = response.body()

    if status / 100 != 2 then
      throw new RuntimeException(
        s"Elasticsearch bulk request failed: HTTP $status\n$body"
      )

    body

  /** Sends NDJSON and checks the Elasticsearch bulk response for item-level errors.
   *
   * Throws if:
   *   - HTTP status is not 2xx
   *   - response JSON contains `"errors":true`
   */
  def sendNdjsonChecked(ndjson: String): String =
    val body = sendNdjson(ndjson)

    if body.contains(""""errors":true""") || body.contains(""""errors" : true""") then
      throw new RuntimeException(
        s"Elasticsearch bulk request completed with item errors.\n$body"
      )

    body

  private def validateNdjson(ndjson: String): Unit =
    Objects.requireNonNull(ndjson, "ndjson must not be null")
    require(ndjson.nonEmpty, "ndjson must not be empty")
    require(
      ndjson.endsWith("\n"),
      "ndjson must end with a newline for the Elasticsearch Bulk API"
    )

object ElasticBulkLogger:

  def fromEnv(
               endpoint: String,
               apiKeyEnvVar: String = "ES_API_KEY",
               connectTimeout: Duration = Duration.ofSeconds(5),
               requestTimeout: Duration = Duration.ofSeconds(15)
             ): ElasticBulkLogger =
    val apiKey =
      Option(System.getenv(apiKeyEnvVar)).getOrElse {
        throw new IllegalArgumentException(
          s"Environment variable '$apiKeyEnvVar' is not set"
        )
      }

    new ElasticBulkLogger(
      endpoint = endpoint,
      apiKey = apiKey,
      connectTimeout = connectTimeout,
      requestTimeout = requestTimeout
    )