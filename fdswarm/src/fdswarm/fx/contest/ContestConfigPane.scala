package fdswarm.fx.contest

import fdswarm.fx.utils.editor.{CaseClassPropertyEditor, IntSpinner}
import jakarta.inject.Inject
import scalafx.beans.property.ObjectProperty
import scalafx.scene.layout.Pane

class ContestConfigPane @Inject()():

  def ss(contestConfig: ObjectProperty[ContestConfig]): ContestConfigPane =
    val configEditor = new CaseClassPropertyEditor(contestConfig)

    configEditor.setCustomEditor("transmitters", new IntSpinner())
    def vertical: Pane =
      configEditor.vertical

    def horizontal: Pane =
      configEditor.horizontal

    def finish(): Unit =
      configEditor.finish()