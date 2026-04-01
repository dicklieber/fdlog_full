package fdswarm.fx.contest

import fdswarm.fx.sections.SectionsProvider
import fdswarm.fx.utils.editor.{
  CallsignCustomField,
  CaseClassPropertyEditor,
  IntSpinner
}
import jakarta.inject.Inject
import scalafx.beans.property.{ObjectProperty, ReadOnlyObjectProperty}
import scalafx.scene.layout.Pane

import scala.Option

class ContestConfigPaneProvider @Inject()(contestCatalog: ContestCatalog,
                                          sectionsProvider: SectionsProvider,
                                          contestConfigManager: ContestConfigManager):

  def pane(): ContestConfigPane =
    val initialConfig = if contestConfigManager.hasConfiguration.value then
      contestConfigManager.contestConfigProperty.value
    else
      // Fallback to a default config if none exists
      ContestConfig(
        contestType = ContestType.WFD, // Or some sensible default
        ourCallsign = fdswarm.model.Callsign("WA9NNN"),
        transmitters = 1,
        ourClass = "A",
        ourSection = "IL"
      )
    new ContestConfigPane(initialConfig, contestCatalog, sectionsProvider)

class ContestConfigPane(initialContestConfig: ContestConfig,
                        contestCatalog: ContestCatalog,
                        sectionsProvider: SectionsProvider):
  private val configEditor: CaseClassPropertyEditor[ContestConfig] = new CaseClassPropertyEditor(initialContestConfig)
  val contestConfigProperty: ReadOnlyObjectProperty[ContestConfig] = configEditor.currentValueProperty
  configEditor.setCustomEditor("contestType", new ContestChooser())
  configEditor.setCustomEditor("transmitters", new IntSpinner())
  configEditor.setCustomEditor("ourCallsign", new CallsignCustomField())
  val contestTypeProperty: ObjectProperty[ContestType] = configEditor.getProperty("contestType")
  configEditor.setCustomEditor("ourClass", contestCatalog.comboBox(contestTypeProperty))
  configEditor.setCustomEditor("ourSection", new SectionComboBox(sectionsProvider))

  val currentContestConfigProperty: ReadOnlyObjectProperty[ContestConfig] = configEditor.currentValueProperty

  def update(contestConfig: ContestConfig): Unit =
    configEditor.update(contestConfig)

  def pane: Pane =
    configEditor.horizontal

  def finish(): ContestConfig =
    configEditor.finish()
    
  