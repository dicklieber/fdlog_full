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

  class ContestConfigPane():
    val contestConfigProperty: ReadOnlyObjectProperty[ContestConfig] = configEditor.currentValueProperty
    private val contestConfig: ContestConfig = contestConfigManager.contestConfig
    private val configEditor: CaseClassPropertyEditor[ContestConfig] = new CaseClassPropertyEditor(contestConfig)
    configEditor.setCustomEditor("contestType", new ContestChooser())
    configEditor.setCustomEditor("transmitters", new IntSpinner())
    configEditor.setCustomEditor("ourCallsign", new CallsignCustomField())
    val contestTypeProperty: ObjectProperty[ContestType] = configEditor.getProperty("contestType")
    configEditor.setCustomEditor("ourClass", contestCatalog.comboBox(contestTypeProperty))
    configEditor.setCustomEditor("ourSection", new SectionComboBox(sectionsProvider))

    def vertical: Pane =
      configEditor.vertical

    def horizontal: Pane =
      configEditor.horizontal

    def finish(): ContestConfig =
      configEditor.finish()
      
    