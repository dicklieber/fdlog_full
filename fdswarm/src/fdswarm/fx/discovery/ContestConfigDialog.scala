package fdswarm.fx.discovery

import fdswarm.fx.contest.{
  ContestConfig,
  ContestCatalog,
  ContestChooser,
  ContestConfigManager,
  SectionComboBox,
  ContestType,
  ExchangePane
}
import fdswarm.fx.sections.SectionsProvider
import fdswarm.fx.utils.StyledDialog
import fdswarm.fx.utils.editor.{
  CallsignCustomField,
  CaseClassPropertyEditor,
  IntSpinner
}
import fdswarm.model.Callsign
import fdswarm.store.QsoStore
import jakarta.inject.Inject
import scalafx.Includes.*
import scalafx.beans.property.{
  BooleanProperty,
  ObjectProperty,
  ReadOnlyObjectProperty
}
import scalafx.scene.control.{ButtonBar, ButtonType}
import scalafx.scene.layout.{Pane, VBox}

class ContestConfigDialog @Inject()(
  contestCatalog: ContestCatalog,
  sectionsProvider: SectionsProvider,
  contestManager: ContestConfigManager,
  qsoStore: QsoStore,
  exchangePane: ExchangePane
)
  extends StyledDialog[ButtonType]:


  private val contestConfigPane: ContestConfigPane =
    new ContestConfigPane(
      contestManager.contestConfigProperty.value,
      contestCatalog,
      sectionsProvider
    )


  title = "Contest Configuration"
  resizable = true
  dialogPane().content = new ContestDetailEditor(
    contestConfigPane,
    exchangePane,
    qsoStore,
    contestManager
  )

  private val updateButtonType = new ButtonType(
    "Update",
    ButtonBar.ButtonData.OKDone
  )

  dialogPane().buttonTypes = Seq(
    updateButtonType,
    ButtonType.Cancel
  )

  private val updateButton = dialogPane().lookupButton(
    updateButtonType
  )
  updateButton.disableProperty().bind(new ContestDetailEditor(
    contestConfigPane,
    exchangePane,
    qsoStore,
    contestManager
  ).isValid.delegate.not())
  updateButton.addEventFilter(
    javafx.event.ActionEvent.ACTION,
    (event: javafx.event.ActionEvent) =>
      if !new ContestDetailEditor(
    contestConfigPane,
    exchangePane,
    qsoStore,
    contestManager
  ).updateContestConfig() then
        event.consume()
  )

class ContestConfigPane(
  initialContestConfig: ContestConfig,
  contestCatalog: ContestCatalog,
  sectionsProvider: SectionsProvider
):
  private val configEditor: CaseClassPropertyEditor[ContestConfig] =
    new CaseClassPropertyEditor(
      initialContestConfig
    )
  val contestConfigProperty: ReadOnlyObjectProperty[ContestConfig] =
    configEditor.currentValueProperty
  configEditor.setCustomEditor(
    "contestType",
    new ContestChooser()
  )
  configEditor.setCustomEditor(
    "transmitters",
    new IntSpinner()
  )
  configEditor.setCustomEditor(
    "ourCallsign",
    new CallsignCustomField()
  )
  val contestTypeProperty: ObjectProperty[ContestType] =
    configEditor.getProperty(
      "contestType"
    )
  configEditor.setCustomEditor(
    "ourClass",
    contestCatalog.comboBox(
      contestTypeProperty
    )
  )
  configEditor.setCustomEditor(
    "ourSection",
    new SectionComboBox(
      sectionsProvider
    )
  )

  val currentContestConfigProperty: ReadOnlyObjectProperty[ContestConfig] =
    configEditor.currentValueProperty
  val isValid: BooleanProperty = BooleanProperty(
    isValidContestConfig(
      currentContestConfigProperty.value
    )
  )

  currentContestConfigProperty.onChange {
    (
      _,
      _,
      updatedConfig
    ) =>
      isValid.value =
        if updatedConfig == null then false
        else isValidContestConfig(
          updatedConfig
        )
  }

  def pane: Pane =
    configEditor.horizontal

  def finish(): ContestConfig =
    configEditor.finish()

  private def isValidContestConfig(
    config: ContestConfig
  ): Boolean =
    config.ourCallsign != null &&
      Callsign.isValid(
        config.ourCallsign.toString
      ) &&
      config.contestType != ContestType.NONE &&
      config.transmitters > 0 &&
      hasValue(
        config.ourClass
      ) &&
      hasValue(
        config.ourSection
      )

  private def hasValue(
    value: String
  ): Boolean =
    value != null && value.trim.nonEmpty && value != "-"
