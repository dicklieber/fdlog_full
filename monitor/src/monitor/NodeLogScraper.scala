package monitor

import fdswarm.logging.LazyStructuredLogging
import fdswarm.util.NodeIdentity
import jakarta.inject.{Inject, Singleton}

import java.net.URI
import java.net.http.{HttpClient, HttpRequest, HttpResponse}
import java.nio.charset.StandardCharsets
import java.time.Duration
import scala.util.{Failure, Try}

@Singleton
final class NodeLogScraper @Inject()(elasticsearchLogIndexer: ElasticsearchLogIndexer) extends LazyStructuredLogging:

  private val httpClient: HttpClient =
    HttpClient
      .newBuilder()
      .connectTimeout(Duration.ofSeconds(5))
      .build()

  /**
   * Scrape log data from a node, index it in Elasticsearch, and return the next log offset.
   *
   * @param nodeIdentity which node to scrape.
   * @param offset where in the log to start scraping.
   */
  def scrapeNode(nodeIdentity: NodeIdentity, offset: Long): Try[IndexOperation] =
    val logUri = nodeLogUri(nodeIdentity, offset)
    val request = HttpRequest
      .newBuilder(logUri)
      .timeout(Duration.ofSeconds(10))
      .GET()
      .build()

    Try {
      val response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray())
      response
    }.flatMap(handleResponse(nodeIdentity, offset, logUri, _))

  private def handleResponse(
      nodeIdentity: NodeIdentity,
      requestedOffset: Long,
      logUri: URI,
      response: HttpResponse[Array[Byte]]
  ): Try[IndexOperation] =
    metadataFrom(response) match
      case Left(message) =>
        logger.warn(
          "Log fetch failed: missing or invalid log API metadata",
          "Node" -> nodeIdentity.toString,
          "Uri" -> logUri.toString,
          "Status" -> response.statusCode(),
          "Details" -> message
        )
        failed(message)

      case Right(metadata) if response.statusCode() == 409 && metadata.truncated =>
        logger.warn(
          "Log offset is beyond the remote log size",
          "Node" -> nodeIdentity.toString,
          "Uri" -> logUri.toString,
          "RequestedOffset" -> requestedOffset,
          "RemoteSize" -> metadata.size,
          "LogId" -> metadata.logId
        )
        failed("Log offset is beyond the remote log size.")

      case Right(metadata) if response.statusCode() < 200 || response.statusCode() >= 300 =>
        logger.warn(
          "Log fetch failed",
          "Node" -> nodeIdentity.toString,
          "Uri" -> logUri.toString,
          "Status" -> response.statusCode(),
          "Body" -> utf8(response.body())
        )
        failed(s"Log fetch failed with status ${response.statusCode()}.")

      case Right(metadata) =>
        indexLogResponse(nodeIdentity, logUri, metadata, response.body())

  private def indexLogResponse(
      nodeIdentity: NodeIdentity,
      logUri: URI,
      metadata: LogApiMetadata,
      logBytes: Array[Byte]
  ): Try[IndexOperation] =
    Try {
      val result = elasticsearchLogIndexer.indexLog(nodeIdentity, metadata, logBytes)
      val operation = IndexOperation(itemCount = result.indexedLines, offset = metadata.to)

      if result.hasFailures then
        logger.warn(
          "Log indexing completed with failures",
          "Node" -> nodeIdentity.toString,
          "Uri" -> logUri.toString,
          "IndexedLines" -> result.indexedLines,
          "AttemptedLines" -> result.attemptedLines,
          "Offset" -> operation.offset,
          "Failures" -> result.failures.mkString("\n")
        )
      else
        logger.debug(
          "Log indexing completed",
          "Node" -> nodeIdentity.toString,
          "Uri" -> logUri.toString,
          "IndexedLines" -> result.indexedLines,
          "AttemptedLines" -> result.attemptedLines,
          "Offset" -> operation.offset
        )

      operation
    }

  private def failed(message: String): Try[IndexOperation] =
    Failure(IllegalStateException(message))

  private def nodeLogUri(nodeIdentity: NodeIdentity, fromByte: Long): URI =
    URI.create(s"http://${nodeIdentity.hostIp}:${nodeIdentity.port}/log?fromByte=$fromByte")

  private def metadataFrom(response: HttpResponse[Array[Byte]]): Either[String, LogApiMetadata] =
    for
      from <- longHeader(response, "X-Log-From")
      to <- longHeader(response, "X-Log-To")
      size <- longHeader(response, "X-Log-Size")
      truncated <- booleanHeader(response, "X-Log-Truncated")
      logId <- stringHeader(response, "X-Log-Id")
    yield LogApiMetadata(from, to, size, logId, truncated)

  private def stringHeader(response: HttpResponse[?], name: String): Either[String, String] =
    val value = response.headers().firstValue(name)
    if value.isPresent then Right(value.get())
    else Left(s"Missing $name header.")

  private def longHeader(response: HttpResponse[?], name: String): Either[String, Long] =
    stringHeader(response, name).flatMap(value =>
      Try(value.toLong).toEither.left.map(_ => s"Invalid $name header: $value")
    )

  private def booleanHeader(response: HttpResponse[?], name: String): Either[String, Boolean] =
    stringHeader(response, name).flatMap(value =>
      value.toLowerCase match
        case "true"  => Right(true)
        case "false" => Right(false)
        case _       => Left(s"Invalid $name header: $value")
    )

  private def utf8(bytes: Array[Byte]): String =
    new String(bytes, StandardCharsets.UTF_8)
