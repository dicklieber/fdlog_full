package fdswarm.fx.contest

import fdswarm.fx.utils.editor.{CallsignCustomField, CaseClassPropertyEditor, IntSpinner}
import jakarta.inject.Inject
import scalafx.beans.property.ObjectProperty
import scalafx.scene.layout.Pane
import fdswarm.fx.sections.SectionsProvider

import scala.Option

class ContestConfigPane @Inject()( contestCatalog:ContestCatalog, sectionsProvider: SectionsProvider):
  private var _configEditor: Option[CaseClassPropertyEditor[ContestConfig]] = None

  def createContestConfigPane(contestConfig: ObjectProperty[ContestConfig]): ContestConfigPane =
    val configEditor = new CaseClassPropertyEditor(contestConfig)
    configEditor.setCustomEditor("contestType", new ContestChooser())
    configEditor.setCustomEditor("transmitters", new IntSpinner())
    configEditor.setCustomEditor("ourCallsign", new CallsignCustomField())
    val contestTypeProperty: ObjectProperty[ContestType] = configEditor.getProperty("contestType")
    configEditor.setCustomEditor("ourClass", contestCatalog.comboBox(contestTypeProperty))
    configEditor.setCustomEditor("ourSection", new SectionComboBox(sectionsProvider))
    _configEditor = Some(configEditor)
    finish()
  def vertical: Pane =
    _configEditor.get.vertical

  def horizontal: Pane =
    _configEditor.get.horizontal

  def finish(): ContestConfigPane = {
    _configEditor.foreach(_.finish())
    this
  }