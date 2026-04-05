package fdswarm.fx.discovery

import fdswarm.fx.contest.ContestConfigPaneProvider
import fdswarm.fx.table.{CellValue, ColumnDef, TableDefinition}
import fdswarm.replication.NodeStatus
import scalafx.scene.control.{Button, Label}

import java.time.Instant

final class ReceivedNodeStatusProvider(private val contestConfigPaneProvider: ContestConfigPaneProvider):
  def update(row: NodeStatus): Unit =
    contestConfigPaneProvider.update(row.statusMessage.contestConfig)

object ReceivedNodeStatusTable extends TableDefinition[NodeStatus, ReceivedNodeStatusProvider]:
  override def title(count: Int): String = s"Node Contest Stations ($count)"

  override def columns: Seq[ColumnDef[NodeStatus, ReceivedNodeStatusProvider, ?]] = Seq(
    ColumnDef[NodeStatus, ReceivedNodeStatusProvider, NodeStatus](
      header = "Use",
      extract = (row, _) => row,
      render = (row, provider) =>
        val button = new Button("Use")
        button.onAction = _ => provider.update(row)
        CellValue.NodeValue(button, "Use"),
      sortable = false,
      resizable = false
    ),
    ColumnDef[NodeStatus, ReceivedNodeStatusProvider, String](
      header = "Node",
      extract = (row, _) => row.nodeIdentity.short,
      render = (value, _) => CellValue.Text(value)
    ),
    ColumnDef[NodeStatus, ReceivedNodeStatusProvider, String](
      header = "Contest",
      extract = (row, _) => row.statusMessage.contestConfig.contestType.toString,
      render = (value, _) => CellValue.Text(value)
    ),
    ColumnDef[NodeStatus, ReceivedNodeStatusProvider, String](
      header = "Our Callsign",
      extract = (row, _) => row.statusMessage.contestConfig.ourCallsign.toString,
      render = (value, _) => CellValue.Text(value)
    ),
    ColumnDef[NodeStatus, ReceivedNodeStatusProvider, Int](
      header = "Transmitters",
      extract = (row, _) => row.statusMessage.contestConfig.transmitters,
      render = (value, _) => CellValue.IntValue(value)
    ),
    ColumnDef[NodeStatus, ReceivedNodeStatusProvider, String](
      header = "Class",
      extract = (row, _) => row.statusMessage.contestConfig.ourClass,
      render = (value, _) => CellValue.Text(value)
    ),
    ColumnDef[NodeStatus, ReceivedNodeStatusProvider, String](
      header = "Section",
      extract = (row, _) => row.statusMessage.contestConfig.ourSection,
      render = (value, _) => CellValue.Text(value)
    ),
    ColumnDef[NodeStatus, ReceivedNodeStatusProvider, Instant](
      header = "When",
      extract = (row, _) => row.statusMessage.contestConfig.stamp,
      render = (value, _) => CellValue.NodeValue(new Label(value.toString), value.toEpochMilli.toString)
    )
  )
