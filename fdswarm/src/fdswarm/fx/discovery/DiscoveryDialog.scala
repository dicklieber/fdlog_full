package fdswarm.fx.discovery

import com.typesafe.scalalogging.LazyLogging
import fdswarm.fx.contest.ContestConfigPane
import fdswarm.fx.utils.{GridColumn, GridColumnAlignment, GridColumnWidth, GridRowBehavior, StyledDialog, TypedGridTableBuilder}
import jakarta.inject.Inject
import scalafx.Includes.*
import scalafx.application.Platform
import scalafx.scene.Node
import scalafx.scene.control.Button
import scalafx.scene.control.ButtonType
import scalafx.scene.control.Dialog
import scalafx.scene.control.ScrollPane
import scalafx.scene.layout.{Region, VBox}

import scala.collection.mutable.ArrayBuffer

class DiscoveryDialog @Inject() (contestDiscovery: ContestDiscovery,
                                 contestConfigPane:ContestConfigPane)
  extends StyledDialog[ButtonType] with LazyLogging:

  private type Ncs = NodeContestStation

  
  private val discovered = ArrayBuffer.empty[Ncs]

  private def textCol(
                       header: String,
                       sortable: Boolean = false,
                       width: GridColumnWidth = GridColumnWidth.flexible(Region.USE_COMPUTED_SIZE),
                       alignment: GridColumnAlignment = GridColumnAlignment.Left,
                       cellStyleClasses: Ncs => Seq[String] = (_: Ncs) => Seq.empty[String]
                     )(
                       value: Ncs => String
                     ): GridColumn[Ncs] =
    GridColumn.text[Ncs](
      header = header,
      value = value,
      cellStyleClasses = cellStyleClasses,
      sortable = sortable,
      alignment = alignment,
      width = width
    )

  private def nodeCol(
                       header: String,
                       width: GridColumnWidth = GridColumnWidth.flexible(Region.USE_COMPUTED_SIZE),
                       alignment: GridColumnAlignment = GridColumnAlignment.Left,
                       cellStyleClasses: Ncs => Seq[String] = (_: Ncs) => Seq.empty[String],
                       sortValue: Option[Ncs => String] = None
                     )(
                       value: Ncs => Node
                     ): GridColumn[Ncs] =
    GridColumn.node[Ncs](
      header = header,
      value = value,
      cellStyleClasses = cellStyleClasses,
      sortValue = sortValue,
      alignment = alignment,
      width = width
    )

  private val table = TypedGridTableBuilder(
    GridRowBehavior[Ncs](
      rowStyleClasses = ncs =>
        if ncs.exchange.trim.isEmpty then Seq("row-warning") else Seq.empty,
      onClick = Some(ncs =>
        logger.info(s"Selected discovery row: $ncs")
      ),
      onDoubleClick = Some(ncs =>
        logger.info(s"Double-clicked discovery row: $ncs")
      )
    ),
    textCol(
      header = "Host IP",
      sortable = true,
      width = GridColumnWidth.fixed(140)
    )(_.nodeIdentity.hostIp),
    textCol(
      header = "Host Name",
      sortable = true,
      width = GridColumnWidth.flexible(180)
    )(_.nodeIdentity.hostName),
    textCol(
      header = "Port",
      sortable = true,
      alignment = GridColumnAlignment.Right,
      width = GridColumnWidth.fixed(80)
    )(_.nodeIdentity.port.toString),
    textCol(
      header = "Contest",
      sortable = true,
      width = GridColumnWidth.flexible(120)
    )(_.discoveryWire.contestConfig.contestType.toString),
    textCol(
      header = "Exchange",
      sortable = true,
      width = GridColumnWidth.flexible(120),
      cellStyleClasses = ncs =>
        if ncs.exchange.trim.isEmpty then Seq("cell-missing") else Seq("cell-ok")
    )(_.exchange),
    textCol(
      header = "Our Call",
      sortable = true,
      width = GridColumnWidth.flexible(120)
    )(_.discoveryWire.contestConfig.ourCallsign.toString),
    textCol(
      header = "Operator",
      sortable = true,
      width = GridColumnWidth.flexible(150)
    )(_.discoveryWire.stationConfig.operator.toString),
    nodeCol(
      header = "Action",
      sortValue = Some(_.nodeIdentity.hostIp),
      alignment = GridColumnAlignment.Center,
      width = GridColumnWidth.fixed(110)
    ) { ncs =>
      new Button("Connect"):
        styleClass += "grid-inline-button"
        onAction = _ =>
          logger.info(s"Connect clicked for ${ncs.nodeIdentity.hostIp}:${ncs.nodeIdentity.port}")
    }
  )

  val vBox = new VBox()
  vBox.children += contestConfigPane.pane
  vBox.children += table.grid
//  private val scrollPane = new ScrollPane:
//    content = table.grid
//    fitToWidth = true
//    prefWidth = 1100
//    prefHeight = 500

  title = "Contest Configuration"
  resizable = true
  dialogPane().content = vBox
  dialogPane().buttonTypes = Seq(ButtonType.OK)

  private def refreshGrid(): Unit =
    table.setItems(discovered)

  refreshGrid()

  contestDiscovery.discoverContest { ncs =>
    logger.info(s"Discovery UI added: $ncs")
    Platform.runLater {
      discovered += ncs
      refreshGrid()
    }
  }