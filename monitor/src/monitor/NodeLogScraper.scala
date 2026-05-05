package monitor

import fdswarm.logging.LazyStructuredLogging
import fdswarm.util.NodeIdentity
import jakarta.inject.{Inject, Singleton}

import java.net.URI
import java.net.http.{HttpClient, HttpRequest, HttpResponse}
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.concurrent.{CompletableFuture, ConcurrentHashMap}
import scala.util.Try
import scala.util.control.NonFatal

@Singleton
final class NodeLogScraper @Inject()(logIndexer: ElasticsearchLogIndexer) extends LazyStructuredLogging:
  private val activeScrapesByNode = ConcurrentHashMap[NodeIdentity, java.lang.Boolean]()
  private val cursorByNode = ConcurrentHashMap[NodeIdentity, LogCursor]()

  private val httpClient: HttpClient =
    HttpClient
      .newBuilder()
      .connectTimeout(Duration.ofSeconds(5))
      .build()

  def scrapeNodes(nodeIdentities: Iterable[NodeIdentity]): Unit =
    nodeIdentities.foreach(scrapeNode)

  def close(): Unit =
    logIndexer.close()

  private def scrapeNode(nodeIdentity: NodeIdentity): Unit =
    if activeScrapesByNode.putIfAbsent(nodeIdentity, java.lang.Boolean.TRUE) == null then
      fetchAndIndexNodeLog(nodeIdentity)
        .whenComplete((_, _) => activeScrapesByNode.remove(nodeIdentity))

  private def fetchAndIndexNodeLog(
      nodeIdentity: NodeIdentity,
      fromByte: Option[Long] = None,
      retryFromStart: Boolean = true
  ): CompletableFuture[Unit] =
    val requestedFromByte = fromByte.getOrElse(nextFromByteFor(nodeIdentity))
    val logUri = nodeLogUri(nodeIdentity, requestedFromByte)
    val fetchRequest = HttpRequest
      .newBuilder(logUri)
      .timeout(Duration.ofSeconds(10))
      .GET()
      .build()

    httpClient
      .sendAsync(fetchRequest, HttpResponse.BodyHandlers.ofByteArray())
      .handle[CompletableFuture[Unit]]((fetchResponse, fetchError) =>
        if fetchError != null then
          logFetchFailed(nodeIdentity, logUri, s"Log fetch failed: ${rootMessage(fetchError)}")
        else if fetchResponse.statusCode() == 409 && retryFromStart then
          forgetCursor(nodeIdentity)
          fetchAndIndexNodeLog(
            nodeIdentity,
            fromByte = Some(0L),
            retryFromStart = false
          )
        else if fetchResponse.statusCode() < 200 || fetchResponse.statusCode() >= 300 then
          logFetchFailed(
            nodeIdentity,
            logUri,
            s"Log fetch failed: HTTP ${fetchResponse.statusCode()} ${utf8(fetchResponse.body())}"
          )
        else
          metadataFrom(fetchResponse) match
            case Left(message) =>
              logFetchFailed(
                nodeIdentity,
                logUri,
                s"Log fetch failed: missing or invalid log API metadata. $message"
              )
            case Right(metadata) =>
              val currentLogId = cursorFor(nodeIdentity).map(_.logId)
              if retryFromStart && metadata.from > 0 && currentLogId.exists(_ != metadata.logId) then
                forgetCursor(nodeIdentity)
                fetchAndIndexNodeLog(
                  nodeIdentity,
                  fromByte = Some(0L),
                  retryFromStart = false
                )
              else
                indexLogResponse(nodeIdentity, logUri, metadata, fetchResponse.body())
      )
      .thenCompose((future: CompletableFuture[Unit]) => future)

  private def indexLogResponse(
      nodeIdentity: NodeIdentity,
      logUri: URI,
      metadata: LogApiMetadata,
      logBytes: Array[Byte]
  ): CompletableFuture[Unit] =
    try
      val result = logIndexer.indexLog(nodeIdentity, metadata, logBytes)
      val cursor = LogCursor(metadata.logId, metadata.to, metadata.size)
      cursorByNode.put(nodeIdentity, cursor)
      if result.hasFailures then
        logger.warn(
          "Log indexing completed with failures",
          "Node" -> nodeIdentity.toString,
          "Uri" -> logUri.toString,
          "IndexedLines" -> result.indexedLines,
          "AttemptedLines" -> result.attemptedLines,
          "Cursor" -> s"${cursor.to}/${cursor.size}",
          "Failures" -> result.failures.mkString("\n")
        )
      else
        logger.info(
          "Log indexing completed",
          "Node" -> nodeIdentity.toString,
          "Uri" -> logUri.toString,
          "IndexedLines" -> result.indexedLines,
          "AttemptedLines" -> result.attemptedLines,
          "Cursor" -> s"${cursor.to}/${cursor.size}",
          "LatestTimestamp" -> result.latestTimestamp.map(_.toString).getOrElse("")
        )
      CompletableFuture.completedFuture(())
    catch
      case NonFatal(e) =>
        logger.error(
          "Could not prepare Elasticsearch bulk request",
          e,
          "Node" -> nodeIdentity.toString,
          "Uri" -> logUri.toString
        )
        CompletableFuture.completedFuture(())

  private def logFetchFailed(nodeIdentity: NodeIdentity, logUri: URI, message: String): CompletableFuture[Unit] =
    logger.warn(
      message,
      "Node" -> nodeIdentity.toString,
      "Uri" -> logUri.toString
    )
    CompletableFuture.completedFuture(())

  private def nodeLogUri(nodeIdentity: NodeIdentity, fromByte: Long): URI =
    URI.create(s"http://${nodeIdentity.hostIp}:${nodeIdentity.port}/log?fromByte=$fromByte")

  private def cursorFor(nodeIdentity: NodeIdentity): Option[LogCursor] =
    Option(cursorByNode.get(nodeIdentity))

  private def nextFromByteFor(nodeIdentity: NodeIdentity): Long =
    cursorFor(nodeIdentity).map(_.to).getOrElse(0L)

  private def forgetCursor(nodeIdentity: NodeIdentity): Unit =
    cursorByNode.remove(nodeIdentity)

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

  private def rootMessage(error: Throwable): String =
    val root =
      Iterator
        .iterate(error)(_.getCause)
        .takeWhile(_ != null)
        .toSeq
        .lastOption
        .getOrElse(error)

    Option(root.getMessage).filter(_.nonEmpty).getOrElse(root.getClass.getName)
