package monitor

import fdswarm.util.NodeIdentity
import jakarta.inject.{Inject, Singleton}
import scalafx.Includes.*
import scalafx.beans.binding.Bindings
import scalafx.beans.property.{ObjectProperty, StringProperty}
import scalafx.collections.ObservableBuffer
import scalafx.geometry.Insets
import scalafx.scene.control.*
import scalafx.scene.layout.BorderPane

import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.{Instant, ZoneId}

@Singleton
final class NodeStatusView @Inject()():
  private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault())

  def content(nodes: ObservableBuffer[NodeData]): BorderPane =
    new BorderPane:
      center = table(nodes)
      padding = Insets(10)

  private def table(nodes: ObservableBuffer[NodeData]): TableView[NodeData] =
    val table: TableView[NodeData] = new TableView[NodeData](nodes):
      columnResizePolicy = TableView.ConstrainedResizePolicy
      columns ++= Seq(
        new TableColumn[NodeData, String]:
          text = "Host"
          cellValueFactory = c => StringProperty(shortHost(c.value.nodeIdentity))
          prefWidth = 200
        ,
        new TableColumn[NodeData, String]:
          text = "Last Status"
          cellValueFactory = c => Bindings.createStringBinding(() => timeFormatter.format(c.value.lastStatus.value), c.value.lastStatus)
          prefWidth = 180
        ,
        new TableColumn[NodeData, Number]:
          text = "Count"
          cellValueFactory = _.value.lastIndexItemCount.delegate
          prefWidth = 60
        ,
        new TableColumn[NodeData, Number]:
          text = "Offset"
          cellValueFactory = _.value.lastIndexOffset.delegate
          prefWidth = 100
        ,
        new TableColumn[NodeData, String]:
          text = "Last Indexed"
          cellValueFactory = c => Bindings.createStringBinding(() => timeFormatter.format(c.value.lastIndexStamp.value), c.value.lastIndexStamp)
          prefWidth = 180
      )
    table

  def shortHost(nodeIdentity: NodeIdentity):String=
    s"${nodeIdentity.hostName}:${nodeIdentity.port}"