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
import scalafx.Includes.*
import scalafx.scene.control.{ButtonBar, ButtonType}
import scalafx.scene.layout.VBox

class ContestDialog @Inject()(
  contestConfigPaneProvider: ContestConfigPaneProvider,
  contestManager: ContestConfigManager,
  qsoStore: QsoStore,
  exchangePane: ExchangePane
)
  extends StyledDialog[ButtonType]:


  private val contestConfigPane: ContestConfigPane = contestConfigPaneProvider.pane()


  val vBox = new VBox()
  private val configBorderPane = new ContestDetailEditor(
    contestConfigPane,
    exchangePane,
    qsoStore,
    contestManager
  )
  vBox.children += configBorderPane

  title = "Contest Configuration"
  resizable = true
  dialogPane().content = vBox

  private val updateButtonType = new ButtonType(
    "Update",
    ButtonBar.ButtonData.OKDone
  )

  dialogPane().buttonTypes = Seq(
    updateButtonType,
    ButtonType.Cancel
  )

  private val updateButton = dialogPane().lookupButton(updateButtonType)
  if updateButton != null then
    updateButton.disableProperty().bind(configBorderPane.isValid.delegate.not())
    updateButton.addEventFilter(
      javafx.event.ActionEvent.ACTION,
      (event: javafx.event.ActionEvent) =>
        if !configBorderPane.updateContestConfig() then
          event.consume()
    )
