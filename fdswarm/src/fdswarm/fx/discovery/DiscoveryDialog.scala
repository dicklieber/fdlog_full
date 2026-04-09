package fdswarm.fx.discovery

import fdswarm.fx.contest.{
  ContestConfigManager,
  ContestConfigPane,
  ContestConfigPaneProvider,
  ExchangePane
}
import fdswarm.fx.utils.StyledDialog
import fdswarm.store.QsoStore
import jakarta.inject.Inject
import javafx.stage.{Stage as JStage}
import scalafx.Includes.*
import scalafx.application.Platform
import scalafx.scene.control.ButtonType
import scalafx.scene.layout.VBox

class DiscoveryDialog @Inject() (contestConfigPaneProvider: ContestConfigPaneProvider,
                                 contestManager: ContestConfigManager,
                                 qsoStore: QsoStore,
                                 exchangePane: ExchangePane)
  extends StyledDialog[ButtonType]:


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

  private def resizeToDiscoveryTable(): Unit =
    val scene = dialogPane().scene.value
    if scene != null then
      scene.getWindow match
        case stage: JStage => stage.sizeToScene()
        case _ => ()
