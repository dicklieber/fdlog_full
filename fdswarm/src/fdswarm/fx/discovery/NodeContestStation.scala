package fdswarm.fx.discovery

import fdswarm.fx.table.{CellValue, ColumnDef, TableDefinition}
import fdswarm.util.NodeIdentity
import io.circe.Codec
import scalafx.scene.control.Label

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

object NodeContestStation extends TableDefinition[NodeContestStation]:
  override def title(count: Int): String = s"Node Contest Stations ($count)"

  override def columns: Seq[ColumnDef[NodeContestStation, ?]] = Seq(
    ColumnDef[NodeContestStation, String](
      header = "Node",
      extract = _.nodeIdentity.short,
      render = f => CellValue.Text(f)
    ),
    ColumnDef[NodeContestStation, String](
      header = "Contest",
      extract = _.discoveryWire.contestConfig.contestType.toString,
      render = f => CellValue.Text(f)
    ),
    ColumnDef[NodeContestStation, String](
      header = "Our Callsign",
      extract = _.discoveryWire.contestConfig.ourCallsign.toString,
      render = f => CellValue.Text(f)
    ),
    ColumnDef[NodeContestStation, Int](
      header = "Transmitters",
      extract = _.discoveryWire.contestConfig.transmitters,
      render = f => CellValue.IntValue(f)
    ),
    ColumnDef[NodeContestStation, String](
      header = "Class",
      extract = _.discoveryWire.contestConfig.ourClass,
      render = f => CellValue.Text(f)
    ),
    ColumnDef[NodeContestStation, String](
      header = "Section",
      extract = _.discoveryWire.contestConfig.ourSection,
      render = f => CellValue.Text(f)
    ),
    ColumnDef[NodeContestStation, Instant](
      header = "When",
      extract = _.discoveryWire.contestConfig.stamp,
      render = f => CellValue.NodeValue(new Label(f.toString), f.toEpochMilli.toString)
    )
  )
