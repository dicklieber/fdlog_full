package monitor

import fdswarm.util.NodeIdentity
import jakarta.inject.{Inject, Singleton}
import scalafx.Includes.*
import scalafx.application.Platform
import scalafx.beans.property.{ObjectProperty, StringProperty}
import scalafx.collections.ObservableBuffer
import scalafx.geometry.Insets
import scalafx.scene.control.*
import scalafx.scene.layout.BorderPane
import scalafx.stage.Window

import java.net.URI
import java.net.http.{HttpClient, HttpRequest, HttpResponse}
import java.time.Duration

@Singleton
final class NodeIdentityDialog @Inject()():
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
          prefWidth = 110
          cellFactory = (_: TableColumn[NodeIdentity, NodeIdentity]) =>
            new TableCell[NodeIdentity, NodeIdentity]:
              private val requestButton = new Button("Request"):
                onAction = _ =>
                  val nodeIdentity = item.value
                  if nodeIdentity != null then requestNodeIdentity(ownerWindow, nodeIdentity)

              private def refresh(nodeIdentity: NodeIdentity): Unit =
                text = null
                graphic =
                  if empty.value || nodeIdentity == null then null
                  else requestButton

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
