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
import java.time.Duration
import scala.util.control.NonFatal

@Singleton
final class NodeIdentityDialog @Inject()(logIndexer: ElasticsearchLogIndexer):
  private val httpClient: HttpClient =
    HttpClient
      .newBuilder()
      .connectTimeout(Duration.ofSeconds(5))
      .build()

  def show(ownerWindow: Window, nodeIdentities: ObservableBuffer[NodeIdentity]): Unit =
    val table = new TableView[NodeIdentity](nodeIdentities):
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
          prefWidth = 260
          cellFactory = (_: TableColumn[NodeIdentity, NodeIdentity]) =>
            new TableCell[NodeIdentity, NodeIdentity]:
              private val requestButton = new Button("Suck Log"):
                onAction = _ =>
                  val nodeIdentity = item.value
                  if nodeIdentity != null then requestNodeIdentity(ownerWindow, nodeIdentity)

              private val indexButton = new Button("Index Log"):
                onAction = _ =>
                  val nodeIdentity = item.value
                  if nodeIdentity != null then fetchAndIndexNodeLog(ownerWindow, nodeIdentity)

              private val buttons = new HBox:
                spacing = 6
                children = Seq(requestButton, indexButton)

              private def refresh(nodeIdentity: NodeIdentity): Unit =
                text = null
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

  private def fetchAndIndexNodeLog(ownerWindow: Window, nodeIdentity: NodeIdentity): Unit =
    val logUri = URI.create(s"http://${nodeIdentity.hostIp}:${nodeIdentity.port}/log")
    val fetchRequest = HttpRequest
      .newBuilder(logUri)
      .timeout(Duration.ofSeconds(10))
      .GET()
      .build()

    httpClient
      .sendAsync(fetchRequest, HttpResponse.BodyHandlers.ofString())
      .handle[Unit]((fetchResponse, fetchError) =>
        if fetchError != null then
          showIndexResult(
            ownerWindow,
            nodeIdentity,
            logUri,
            s"Log fetch failed:\n${rootMessage(fetchError)}"
          )
        else if fetchResponse.statusCode() < 200 || fetchResponse.statusCode() >= 300 then
          showIndexResult(
            ownerWindow,
            nodeIdentity,
            logUri,
            s"Log fetch failed: HTTP ${fetchResponse.statusCode()}\n\n${fetchResponse.body()}"
          )
        else
          indexLogResponse(ownerWindow, nodeIdentity, logUri, fetchResponse.body())
      )

  private def indexLogResponse(
      ownerWindow: Window,
      nodeIdentity: NodeIdentity,
      logUri: URI,
      logText: String
  ): Unit =
    try
      val result = logIndexer.indexLog(logText)
      val failures =
        if result.hasFailures then
          "\n\nFailures:\n" + result.failures.mkString("\n")
        else ""
      showIndexResult(
        ownerWindow,
        nodeIdentity,
        URI.create(s"${result.elasticsearchUrl.stripSuffix("/")}/${result.index}"),
        s"Fetched log from $logUri.\n" +
          s"Indexed ${result.indexedLines} of ${result.attemptedLines} JSON log lines " +
          s"to ${result.elasticsearchUrl}/${result.index}.$failures"
      )
    catch
      case NonFatal(e) =>
        showIndexResult(
          ownerWindow,
          nodeIdentity,
          logUri,
          s"Could not prepare Elasticsearch bulk request:\n${rootMessage(e)}"
        )

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
