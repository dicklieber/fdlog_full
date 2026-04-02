package fdswarm.fx.discovery

import com.typesafe.scalalogging.LazyLogging
import fdswarm.fx.contest.{
  ContestConfigManager,
  ContestConfigPane,
  ContestConfigPaneProvider,
  ExchangePane
}
import fdswarm.fx.utils.{ObservableScalaMap, StyledDialog}
import fdswarm.store.QsoStore
import fdswarm.util.NodeIdentity
import jakarta.inject.Inject
import scalafx.Includes.*
import scalafx.application.Platform
import scalafx.scene.control.ButtonType
import scalafx.scene.layout.VBox

class DiscoveryDialog @Inject() (contestDiscovery: ContestDiscovery,
                                 contestConfigPaneProvider: ContestConfigPaneProvider,
                                 contestManager: ContestConfigManager,
                                 qsoStore: QsoStore,
                                 exchangePane: ExchangePane)
  extends StyledDialog[ButtonType] with LazyLogging:


  
  private val discoveredNodes = new ObservableScalaMap[NodeIdentity, NodeContestStation]
  private val contestConfigPane: ContestConfigPane = contestConfigPaneProvider.pane()
  private val discoveryTable = new DiscoveryTable(contestConfigPaneProvider)


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
  } }


  contestDiscovery.discoverContest { ncs =>
    logger.debug(s"Discovery UI added: $ncs")
    Platform.runLater {
      discoveredNodes.put(ncs.nodeIdentity, ncs)
      discoveryTable.setItems(discoveredNodes.values)
    }
  }
