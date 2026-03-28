package fdswarm.fx.contest

import fdswarm.fx.utils.editor.{CallsignCustomField, CaseClassPropertyEditor, IntSpinner}
import jakarta.inject.Inject
import scalafx.beans.property.ObjectProperty
import scalafx.scene.layout.Pane

import scala.Option

class ContestConfigPane @Inject()():
  private var _configEditor: Option[CaseClassPropertyEditor[ContestConfig]] = None

  def ss(contestConfig: ObjectProperty[ContestConfig]): ContestConfigPane =
    val configEditor = new CaseClassPropertyEditor(contestConfig)
    configEditor.setCustomEditor("contestType", new ContestChooser())
    configEditor.setCustomEditor("transmitters", new IntSpinner())
    configEditor.setCustomEditor("ourCallsign", new CallsignCustomField())
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