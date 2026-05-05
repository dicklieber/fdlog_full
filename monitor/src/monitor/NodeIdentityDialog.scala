package monitor

import fdswarm.util.NodeIdentity
import jakarta.inject.{Inject, Singleton}
import scalafx.Includes.*
import scalafx.beans.property.{ObjectProperty, StringProperty}
import scalafx.collections.ObservableBuffer
import scalafx.geometry.Insets
import scalafx.scene.control.*
import scalafx.scene.layout.BorderPane
import scalafx.stage.Window

@Singleton
final class NodeIdentityDialog @Inject()():
  def content(nodeIdentities: ObservableBuffer[NodeIdentity]): BorderPane =
    new BorderPane:
      center = table(nodeIdentities)
      padding = Insets(10)

  def show(ownerWindow: Window, nodeIdentities: ObservableBuffer[NodeIdentity]): Unit =
    val dialog = new Dialog[Unit]():
      initOwner(ownerWindow)
      title = "Node Identities"
      headerText = "Known UDP Node Identities"
      resizable = true

    dialog.dialogPane().content = content(nodeIdentities)
    dialog.dialogPane().buttonTypes = Seq.empty
    dialog.showAndWait()

  private def table(nodeIdentities: ObservableBuffer[NodeIdentity]): TableView[NodeIdentity] =
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
      )
    table
