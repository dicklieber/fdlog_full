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
import java.time.Instant
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap
import scala.jdk.CollectionConverters.*
import scala.util.Try

final case class LogIndexResult(
    elasticsearchUrl: String,
    index: String,
    attemptedLines: Int,
    indexedLines: Int,
    latestTimestamp: Option[Instant],
    failures: Seq[String]
):
  def hasFailures: Boolean = failures.nonEmpty

@Singleton
final class ElasticsearchLogIndexer @Inject()(config: Config):
  private val timestampField = """"@timestamp":"([^"]+)"""".r
  private val latestTimestampByNode = ConcurrentHashMap[NodeIdentity, Instant]()

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

  def latestTimestampFor(nodeIdentity: NodeIdentity): Option[Instant] =
    Option(latestTimestampByNode.get(nodeIdentity))

  def indexLog(nodeIdentity: NodeIdentity, logText: String): LogIndexResult =
    val lines = jsonLogLines(logText)
    if lines.isEmpty then
      LogIndexResult(
        elasticsearchUrl = elasticsearchUrl,
        index = elasticsearchIndex,
        attemptedLines = 0,
        indexedLines = 0,
        latestTimestamp = None,
        failures = Seq("Fetched log, but found no JSON log lines to index.")
      )
    else
      val latestTimestamp = timestampFromLastLine(lines)
      val requestBuilder = new BulkRequest.Builder()
      lines.foreach { line =>
        val data = BinaryData.of(line.getBytes(StandardCharsets.UTF_8), ContentType.APPLICATION_JSON)
        requestBuilder.operations(op =>
          op.index(idx =>
            idx
              .index(elasticsearchIndex)
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

      latestTimestamp.foreach(timestamp => latestTimestampByNode.put(nodeIdentity, timestamp))

      LogIndexResult(
        elasticsearchUrl = elasticsearchUrl,
        index = elasticsearchIndex,
        attemptedLines = lines.size,
        indexedLines = response.items().size() - failures.size,
        latestTimestamp = latestTimestamp,
        failures = failures
      )

  def close(): Unit =
    transport.close()

  private def jsonLogLines(logText: String): Seq[String] =
    logText
      .linesIterator
      .map(_.trim)
      .filter(line => line.nonEmpty && line.startsWith("{"))
      .toSeq

  private def timestampFromLastLine(lines: Seq[String]): Option[Instant] =
    lines.lastOption
      .flatMap(line => timestampField.findFirstMatchIn(line).map(_.group(1)))
      .flatMap(timestamp => Try(Instant.parse(timestamp)).toOption)

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
