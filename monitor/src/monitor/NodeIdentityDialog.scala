package monitor

import fdswarm.util.NodeIdentity
import jakarta.inject.{Inject, Singleton}
import scalafx.Includes.*
import scalafx.application.Platform
import scalafx.beans.property.{ObjectProperty, StringProperty}
import scalafx.collections.ObservableBuffer
import scalafx.geometry.Insets
import scalafx.scene.control.*
import scalafx.scene.layout.{BorderPane, HBox}
import scalafx.stage.Window

import java.net.URI
import java.net.http.{HttpClient, HttpRequest, HttpResponse}
import java.nio.charset.StandardCharsets
import java.time.Duration
import scala.util.control.NonFatal
import scala.util.Try

@Singleton
final class NodeIdentityDialog @Inject()(logIndexer: ElasticsearchLogIndexer):
  private val httpClient: HttpClient =
    HttpClient
      .newBuilder()
      .connectTimeout(Duration.ofSeconds(5))
      .build()

  def show(ownerWindow: Window, nodeIdentities: ObservableBuffer[NodeIdentity]): Unit =
    var refreshRows: () => Unit = () => ()
    val table: TableView[NodeIdentity] = new TableView[NodeIdentity](nodeIdentities):
      columnResizePolicy = TableView.ConstrainedResizePolicy
      columns ++= Seq(
        new TableColumn[NodeIdentity, String]:
          text = "Host"
          cellValueFactory = c => StringProperty(c.value.hostName)
          prefWidth = 180
        ,
        new TableColumn[NodeIdentity, String]:
          text = "IP"
          cellValueFactory = c => StringProperty(c.value.hostIp)
          prefWidth = 150
        ,
        new TableColumn[NodeIdentity, Int]:
          text = "Port"
          cellValueFactory = c => ObjectProperty(c.value.port)
          prefWidth = 80
        ,
        new TableColumn[NodeIdentity, String]:
          text = "Instance"
          cellValueFactory = c => StringProperty(c.value.instanceId)
          prefWidth = 180
        ,
        new TableColumn[NodeIdentity, String]:
          text = "Node"
          cellValueFactory = c => StringProperty(c.value.toString)
          prefWidth = 320
        ,
        new TableColumn[NodeIdentity, NodeIdentity]:
          text = ""
          cellValueFactory = c => ObjectProperty(c.value)
          prefWidth = 340
          cellFactory = (_: TableColumn[NodeIdentity, NodeIdentity]) =>
            new TableCell[NodeIdentity, NodeIdentity]:
              private val requestButton = new Button("Suck Log"):
                onAction = _ =>
                  val nodeIdentity = item.value
                  if nodeIdentity != null then requestNodeIdentity(ownerWindow, nodeIdentity)

              private val indexButton = new Button("Index Log"):
                onAction = _ =>
                  val nodeIdentity = item.value
                  if nodeIdentity != null then fetchAndIndexNodeLog(ownerWindow, nodeIdentity, refreshRows())

              private val resetCursorButton = new Button("Reset Cursor"):
                onAction = _ =>
                  val nodeIdentity = item.value
                  if nodeIdentity != null then
                    logIndexer.forgetCursor(nodeIdentity)
                    refreshRows()

              private val buttons = new HBox:
                spacing = 6
                children = Seq(requestButton, indexButton, resetCursorButton)

              private def refresh(nodeIdentity: NodeIdentity): Unit =
                text = null
                resetCursorButton.disable = nodeIdentity == null || logIndexer.cursorFor(nodeIdentity).isEmpty
                graphic =
                  if empty.value || nodeIdentity == null then null
                  else buttons

              item.onChange { (_, _, nodeIdentity) =>
                refresh(nodeIdentity)
              }
              empty.onChange { (_, _, _) =>
                refresh(item.value)
              }
              refresh(item.value)
      )
    refreshRows = () => table.refresh()

    val dialog = new Dialog[Unit]():
      initOwner(ownerWindow)
      title = "Node Identities"
      headerText = "Known UDP Node Identities"
      resizable = true

    dialog.dialogPane().content = new BorderPane:
      center = table
      padding = Insets(10)
      prefWidth = 900
      prefHeight = 450
    dialog.dialogPane().buttonTypes = Seq(ButtonType.Close)
    dialog.showAndWait()

  private def requestNodeIdentity(ownerWindow: Window, nodeIdentity: NodeIdentity): Unit =
    val uri = URI.create(s"http://${nodeIdentity.hostIp}:${nodeIdentity.port}/log")
    val request = HttpRequest
      .newBuilder(uri)
      .timeout(Duration.ofSeconds(10))
      .GET()
      .build()

    httpClient
      .sendAsync(request, HttpResponse.BodyHandlers.ofString())
      .handle[Unit]((response, error) =>
        Platform.runLater(() =>
          if error != null then
            showResponseDialog(
              ownerWindow,
              nodeIdentity,
              uri,
              s"Request failed:\n${rootMessage(error)}"
            )
          else
            showResponseDialog(
              ownerWindow,
              nodeIdentity,
              uri,
              s"HTTP ${response.statusCode()}\n\n${response.body()}"
            )
        )
      )

  private def fetchAndIndexNodeLog(
      ownerWindow: Window,
      nodeIdentity: NodeIdentity,
      afterIndex: => Unit = (),
      fromByte: Option[Long] = None,
      retryFromStart: Boolean = true
  ): Unit =
    val requestedFromByte = fromByte.getOrElse(logIndexer.nextFromByteFor(nodeIdentity))
    val logUri = nodeLogUri(nodeIdentity, requestedFromByte)
    val fetchRequest = HttpRequest
      .newBuilder(logUri)
      .timeout(Duration.ofSeconds(10))
      .GET()
      .build()

    httpClient
      .sendAsync(fetchRequest, HttpResponse.BodyHandlers.ofByteArray())
      .handle[Unit]((fetchResponse, fetchError) =>
        if fetchError != null then
          showIndexResult(
            ownerWindow,
            nodeIdentity,
            logUri,
            s"Log fetch failed:\n${rootMessage(fetchError)}"
          )
        else if fetchResponse.statusCode() == 409 && retryFromStart then
          logIndexer.forgetCursor(nodeIdentity)
          fetchAndIndexNodeLog(
            ownerWindow,
            nodeIdentity,
            afterIndex,
            fromByte = Some(0L),
            retryFromStart = false
          )
        else if fetchResponse.statusCode() < 200 || fetchResponse.statusCode() >= 300 then
          showIndexResult(
            ownerWindow,
            nodeIdentity,
            logUri,
            s"Log fetch failed: HTTP ${fetchResponse.statusCode()}\n\n${utf8(fetchResponse.body())}"
          )
        else
          metadataFrom(fetchResponse) match
            case Left(message) =>
              showIndexResult(
                ownerWindow,
                nodeIdentity,
                logUri,
                s"Log fetch failed: missing or invalid log API metadata.\n$message"
              )
            case Right(metadata) =>
              val currentLogId = logIndexer.cursorFor(nodeIdentity).map(_.logId)
              if retryFromStart && metadata.from > 0 && currentLogId.exists(_ != metadata.logId) then
                logIndexer.forgetCursor(nodeIdentity)
                fetchAndIndexNodeLog(
                  ownerWindow,
                  nodeIdentity,
                  afterIndex,
                  fromByte = Some(0L),
                  retryFromStart = false
                )
              else
                indexLogResponse(ownerWindow, nodeIdentity, logUri, metadata, fetchResponse.body(), afterIndex)
      )

  private def indexLogResponse(
      ownerWindow: Window,
      nodeIdentity: NodeIdentity,
      logUri: URI,
      metadata: LogApiMetadata,
      logBytes: Array[Byte],
      afterIndex: => Unit
  ): Unit =
    try
      val result = logIndexer.indexLog(nodeIdentity, metadata, logBytes)
      Platform.runLater(() => afterIndex)
      val failures =
        if result.hasFailures then
          "\n\nFailures:\n" + result.failures.mkString("\n")
        else ""
      val timestamp =
        result.latestTimestamp
          .map(value => s"\nLatest log timestamp for this node: ${value.toString}")
          .getOrElse("")
      val cursor =
        s"\nLog cursor: ${result.cursor.to}/${result.cursor.size} bytes."
      showIndexResult(
        ownerWindow,
        nodeIdentity,
        URI.create(s"${result.elasticsearchUrl.stripSuffix("/")}/${result.index}"),
        s"Fetched log from $logUri.\n" +
          s"Indexed ${result.indexedLines} of ${result.attemptedLines} JSON log lines " +
          s"to ${result.elasticsearchUrl}/${result.index}.$cursor$timestamp$failures"
      )
    catch
      case NonFatal(e) =>
        showIndexResult(
          ownerWindow,
          nodeIdentity,
          logUri,
          s"Could not prepare Elasticsearch bulk request:\n${rootMessage(e)}"
        )

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

  private def showIndexResult(
      ownerWindow: Window,
      nodeIdentity: NodeIdentity,
      uri: URI,
      responseText: String
  ): Unit =
    Platform.runLater(() =>
      showResponseDialog(
        ownerWindow,
        nodeIdentity,
        uri,
        responseText
      )
    )

  private def showResponseDialog(
      ownerWindow: Window,
      nodeIdentity: NodeIdentity,
      uri: URI,
      responseText: String
  ): Unit =
    val responseArea = new TextArea(responseText):
      editable = false
      wrapText = false

    val dialog = new Dialog[Unit]():
      initOwner(ownerWindow)
      title = "Node Identity Response"
      headerText = s"${nodeIdentity.hostName} ${uri.toString}"
      resizable = true

    dialog.dialogPane().content = new BorderPane:
      center = responseArea
      padding = Insets(10)
      prefWidth = 700
      prefHeight = 450
    dialog.dialogPane().buttonTypes = Seq(ButtonType.Close)
    dialog.showAndWait()

  private def rootMessage(error: Throwable): String =
    val root =
      Iterator
        .iterate(error)(_.getCause)
        .takeWhile(_ != null)
        .toSeq
        .lastOption
        .getOrElse(error)

    Option(root.getMessage).filter(_.nonEmpty).getOrElse(root.getClass.getName)
