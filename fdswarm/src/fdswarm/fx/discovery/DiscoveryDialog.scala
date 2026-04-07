package fdswarm.fx.discovery

import com.typesafe.scalalogging.LazyLogging
import fdswarm.fx.contest.{
  ContestConfigManager,
  ContestConfigPane,
  ContestConfigPaneProvider,
  ExchangePane
}
import fdswarm.fx.utils.StyledDialog
import fdswarm.replication.status.SwarmData
import fdswarm.store.QsoStore
import jakarta.inject.Inject
import javafx.stage.{Stage as JStage}
import scalafx.Includes.*
import scalafx.application.Platform
import scalafx.scene.control.ButtonType
import scalafx.scene.layout.VBox

class DiscoveryDialog @Inject() (contestDiscovery: ContestDiscovery,
                                 contestConfigPaneProvider: ContestConfigPaneProvider,
                                 contestManager: ContestConfigManager,
                                 qsoStore: QsoStore,
                                 exchangePane: ExchangePane,
                                 swarmData: SwarmData)
  extends StyledDialog[ButtonType] with LazyLogging:


  private val contestConfigPane: ContestConfigPane = contestConfigPaneProvider.pane()
  private val discoveryTable = new DiscoveryTable(contestConfigPaneProvider, resizeToDiscoveryTable)


  val vBox = new VBox()
  private val configBorderPane = new ContestDetailEditor(contestConfigPane, exchangePane, qsoStore, contestManager)
  vBox.children += configBorderPane
  vBox.children += discoveryTable

  title = "Contest Configuration"
  resizable = true
  dialogPane().content = vBox
  dialogPane().buttonTypes = Seq(ButtonType.Close)
  onShowing.value = { (_: javafx.scene.control.DialogEvent) => Platform.runLater {
    val btn = dialogPane().lookupButton(ButtonType.Close)
    if (btn != null) {
      btn.setVisible(false)
    }
    resizeToDiscoveryTable()
  } }


  contestDiscovery.discoverContest().foreach { receivedNodeStatus =>
    logger.debug(s"Discovery UI added: $receivedNodeStatus")
    Platform.runLater {
      discoveryTable.setItems(swarmData.nodeMap.values.toSeq)
    }
  }

  private def resizeToDiscoveryTable(): Unit =
    val scene = dialogPane().scene.value
    if scene != null then
      scene.getWindow match
        case stage: JStage => stage.sizeToScene()
        case _ => ()
