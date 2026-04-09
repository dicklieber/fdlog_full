package fdswarm.fx.contest

import fdswarm.fx.sections.SectionsProvider
import fdswarm.model.Callsign
import fdswarm.fx.utils.editor.{
  CallsignCustomField,
  CaseClassPropertyEditor,
  IntSpinner
}
import jakarta.inject.Inject
import scalafx.beans.property.{
  BooleanProperty,
  ObjectProperty,
  ReadOnlyObjectProperty
}
import scalafx.scene.layout.Pane

import scala.Option

class ContestConfigPaneProvider @Inject()(
  contestCatalog: ContestCatalog,
  sectionsProvider: SectionsProvider,
  contestConfigManager: ContestConfigManager
):
  private var currentPane: ContestConfigPane | Null = null

  def pane(): ContestConfigPane =
    val initialConfig = contestConfigManager.contestConfigProperty.value
    val pane = new ContestConfigPane(
      initialConfig,
      contestCatalog,
      sectionsProvider
    )
    currentPane = pane
    pane

  def update(contestConfig: ContestConfig): Unit =
    if currentPane != null then
      currentPane.nn.update(contestConfig)

class ContestConfigPane(
  initialContestConfig: ContestConfig,
  contestCatalog: ContestCatalog,
  sectionsProvider: SectionsProvider
):
  private val configEditor: CaseClassPropertyEditor[ContestConfig] =
    new CaseClassPropertyEditor(initialContestConfig)
  val contestConfigProperty: ReadOnlyObjectProperty[ContestConfig] = configEditor.currentValueProperty
  configEditor.setCustomEditor("contestType", new ContestChooser())
  configEditor.setCustomEditor("transmitters", new IntSpinner())
  configEditor.setCustomEditor("ourCallsign", new CallsignCustomField())
  val contestTypeProperty: ObjectProperty[ContestType] = configEditor.getProperty("contestType")
  configEditor.setCustomEditor("ourClass", contestCatalog.comboBox(contestTypeProperty))
  configEditor.setCustomEditor("ourSection", new SectionComboBox(sectionsProvider))

  val currentContestConfigProperty: ReadOnlyObjectProperty[ContestConfig] = configEditor.currentValueProperty
  val isValid: BooleanProperty = BooleanProperty(
    isValidContestConfig(currentContestConfigProperty.value)
  )

  currentContestConfigProperty.onChange {
    (
      _,
      _,
      updatedConfig
    ) =>
      isValid.value =
        if updatedConfig == null then false
        else isValidContestConfig(updatedConfig)
  }

  def update(contestConfig: ContestConfig): Unit =
    configEditor.update(contestConfig)

  def pane: Pane =
    configEditor.horizontal

  def finish(): ContestConfig =
    configEditor.finish()

  private def isValidContestConfig(
    config: ContestConfig
  ): Boolean =
    config.ourCallsign != null &&
      Callsign.isValid(config.ourCallsign.toString) &&
      config.contestType != ContestType.NONE &&
      config.transmitters > 0 &&
      hasValue(config.ourClass) &&
      hasValue(config.ourSection)

  private def hasValue(
    value: String
  ): Boolean =
    value != null && value.trim.nonEmpty && value != "-"
