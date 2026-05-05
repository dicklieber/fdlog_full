package monitor

import co.elastic.clients.elasticsearch.ElasticsearchClient
import co.elastic.clients.elasticsearch.core.BulkRequest
import co.elastic.clients.json.jackson.JacksonJsonpMapper
import co.elastic.clients.transport.rest_client.RestClientTransport
import co.elastic.clients.util.{BinaryData, ContentType}
import com.typesafe.config.Config
import fdswarm.util.NodeIdentity
import jakarta.inject.{Inject, Singleton}
import org.apache.http.HttpHost
import org.apache.http.message.BasicHeader
import org.elasticsearch.client.RestClient

import java.nio.charset.StandardCharsets
import java.util.Base64
import scala.collection.mutable.ArrayBuffer
import scala.jdk.CollectionConverters.*

final case class LogApiMetadata(
    from: Long,
    to: Long,
    size: Long,
    logId: String,
    truncated: Boolean
)

final case class LogIndexResult(
    elasticsearchUrl: String,
    index: String,
    attemptedLines: Int,
    indexedLines: Int,
    failures: Seq[String]
):
  def hasFailures: Boolean = failures.nonEmpty

@Singleton
final class ElasticsearchLogIndexer @Inject()(config: Config):
  private val elasticsearchUrl: String =
    if config.hasPath("monitor.elasticsearch.url") then config.getString("monitor.elasticsearch.url")
    else "http://localhost:9200"

  private val elasticsearchIndex: String =
    if config.hasPath("monitor.elasticsearch.index") then config.getString("monitor.elasticsearch.index")
    else "fdswarm-logs"

  private val restClient: RestClient =
    val builder = RestClient.builder(HttpHost.create(elasticsearchUrl))
    authHeader.foreach(header => builder.setDefaultHeaders(Array(header)))
    builder.build()

  private val transport: RestClientTransport =
    new RestClientTransport(restClient, new JacksonJsonpMapper())

  private val client: ElasticsearchClient =
    new ElasticsearchClient(transport)

  def indexLog(nodeIdentity: NodeIdentity, metadata: LogApiMetadata, logBytes: Array[Byte]): LogIndexResult =
    val lines = jsonLogLines(metadata.from, logBytes)
    if lines.isEmpty then
      LogIndexResult(
        elasticsearchUrl = elasticsearchUrl,
        index = elasticsearchIndex,
        attemptedLines = 0,
        indexedLines = 0,
        failures = Seq.empty
      )
    else
      val requestBuilder = new BulkRequest.Builder()
      lines.foreach { logLine =>
        val data = BinaryData.of(logLine.line.getBytes(StandardCharsets.UTF_8), ContentType.APPLICATION_JSON)
        requestBuilder.operations(op =>
          op.index(idx =>
            idx
              .index(elasticsearchIndex)
              .id(documentId(nodeIdentity, metadata.logId, logLine.offset))
              .document(data)
          )
        )
      }

      val response = client.bulk(requestBuilder.build())
      val failures = response
        .items()
        .asScala
        .zipWithIndex
        .collect {
          case (item, index) if item.error() != null =>
            s"line ${index + 1}: ${item.error().reason()}"
        }
        .toSeq

      LogIndexResult(
        elasticsearchUrl = elasticsearchUrl,
        index = elasticsearchIndex,
        attemptedLines = lines.size,
        indexedLines = response.items().size() - failures.size,
        failures = failures
      )

  def close(): Unit =
    transport.close()

  private case class JsonLogLine(offset: Long, line: String)

  private def jsonLogLines(fromByte: Long, logBytes: Array[Byte]): Seq[JsonLogLine] =
    val lines = ArrayBuffer.empty[JsonLogLine]
    var lineStart = 0
    var index = 0
    while index < logBytes.length do
      if logBytes(index) == '\n'.toByte then
        addJsonLogLine(fromByte, logBytes, lineStart, index, lines)
        lineStart = index + 1
      index += 1
    lines.toSeq

  private def addJsonLogLine(
      fromByte: Long,
      logBytes: Array[Byte],
      lineStart: Int,
      lineEnd: Int,
      lines: ArrayBuffer[JsonLogLine]
  ): Unit =
    val length =
      if lineEnd > lineStart && logBytes(lineEnd - 1) == '\r'.toByte then lineEnd - lineStart - 1
      else lineEnd - lineStart
    if length > 0 then
      val line = new String(logBytes, lineStart, length, StandardCharsets.UTF_8).trim
      if line.nonEmpty && line.startsWith("{") then
        lines += JsonLogLine(fromByte + lineStart, line)

  private def documentId(nodeIdentity: NodeIdentity, logId: String, offset: Long): String =
    Base64
      .getUrlEncoder
      .withoutPadding()
      .encodeToString(s"$nodeIdentity|$logId|$offset".getBytes(StandardCharsets.UTF_8))

  private def authHeader: Option[BasicHeader] =
    configValue("monitor.elasticsearch.apiKey")
      .map(value => new BasicHeader("Authorization", s"ApiKey $value"))
      .orElse(
        configValue("monitor.elasticsearch.bearerToken")
          .map(value => new BasicHeader("Authorization", s"Bearer $value"))
      )
      .orElse(basicAuthHeader)

  private def basicAuthHeader: Option[BasicHeader] =
    for
      username <- configValue("monitor.elasticsearch.username")
      password <- configValue("monitor.elasticsearch.password")
    yield
      val encoded = Base64
        .getEncoder
        .encodeToString(s"$username:$password".getBytes(StandardCharsets.UTF_8))
      new BasicHeader("Authorization", s"Basic $encoded")

  private def configValue(path: String): Option[String] =
    if config.hasPath(path) then
      Option(config.getString(path)).map(_.trim).filter(_.nonEmpty)
    else None
