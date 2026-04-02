package fdswarm.fx.discovery

import fdswarm.fx.contest.ContestConfigPaneProvider
import fdswarm.fx.table.{CellValue, ColumnDef, TableDefinition}
import fdswarm.util.NodeIdentity
import io.circe.Codec
import scalafx.scene.control.{Button, Label}

import java.time.Instant

/** Combines the ContestStation as received from other nodes with the
  * NodeIdentity of the node, as extracted from the UDPHeader.
  */
case class NodeContestStation(
    nodeIdentity: NodeIdentity,
    discoveryWire: DiscoveryWire
) derives Codec.AsObject:

  val exchange: String =
    discoveryWire.contestConfig.exchange

final class NodeContestStationProvider(private val contestConfigPaneProvider: ContestConfigPaneProvider):
  def update(row: NodeContestStation): Unit =
    contestConfigPaneProvider.update(row.discoveryWire.contestConfig)

object NodeContestStation extends TableDefinition[NodeContestStation, NodeContestStationProvider]:
  override def title(count: Int): String = s"Node Contest Stations ($count)"

  override def columns: Seq[ColumnDef[NodeContestStation, NodeContestStationProvider, ?]] = Seq(
    ColumnDef[NodeContestStation, NodeContestStationProvider, NodeContestStation](
      header = "Use",
      extract = (row, _) => row,
      render = (row, provider) =>
        val button = new Button("Use")
        button.onAction = _ => provider.update(row)
        CellValue.NodeValue(button, "Use"),
      sortable = false,
      resizable = false
    ),
    ColumnDef[NodeContestStation, NodeContestStationProvider, String](
      header = "Node",
      extract = (row, _) => row.nodeIdentity.short,
      render = (f, _) => CellValue.Text(f)
    ),
    ColumnDef[NodeContestStation, NodeContestStationProvider, String](
      header = "Contest",
      extract = (row, _) => row.discoveryWire.contestConfig.contestType.toString,
      render = (f, _) => CellValue.Text(f)
    ),
    ColumnDef[NodeContestStation, NodeContestStationProvider, String](
      header = "Our Callsign",
      extract = (row, _) => row.discoveryWire.contestConfig.ourCallsign.toString,
      render = (f, _) => CellValue.Text(f)
    ),
    ColumnDef[NodeContestStation, NodeContestStationProvider, Int](
      header = "Transmitters",
      extract = (row, _) => row.discoveryWire.contestConfig.transmitters,
      render = (f, _) => CellValue.IntValue(f)
    ),
    ColumnDef[NodeContestStation, NodeContestStationProvider, String](
      header = "Class",
      extract = (row, _) => row.discoveryWire.contestConfig.ourClass,
      render = (f, _) => CellValue.Text(f)
    ),
    ColumnDef[NodeContestStation, NodeContestStationProvider, String](
      header = "Section",
      extract = (row, _) => row.discoveryWire.contestConfig.ourSection,
      render = (f, _) => CellValue.Text(f)
    ),
    ColumnDef[NodeContestStation, NodeContestStationProvider, Instant](
      header = "When",
      extract = (row, _) => row.discoveryWire.contestConfig.stamp,
      render = (f, _) => CellValue.NodeValue(new Label(f.toString), f.toEpochMilli.toString)
    )
  )
