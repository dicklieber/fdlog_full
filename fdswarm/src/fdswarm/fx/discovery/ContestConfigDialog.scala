package fdswarm.fx.discovery

import fdswarm.fx.contest.*
import fdswarm.fx.sections.SectionsProvider
import fdswarm.fx.utils.StyledDialog
import fdswarm.fx.utils.editor.{CallsignCustomField, CaseClassPropertyEditor, IntSpinner}
import jakarta.inject.Inject
import scalafx.Includes.*
import scalafx.beans.property.{ObjectProperty, ReadOnlyObjectProperty}
import scalafx.scene.control.ButtonType
import scalafx.scene.layout.VBox

import java.time.Instant

/**
 * Dialog for configuring contest settings.
 */
class ContestConfigDialog @Inject() (
    contestCatalog: ContestCatalog,
    sectionsProvider: SectionsProvider,
    contestManager: ContestConfigManager,
    exchangePane: ExchangePane)
    extends StyledDialog[ButtonType]:

  private val configEditor: CaseClassPropertyEditor[ContestConfig] =
    new CaseClassPropertyEditor(contestManager.contestConfigProperty.value)

  configEditor.setCustomEditor("contestType", new ContestChooser())
  configEditor.setCustomEditor("transmitters", new IntSpinner())
  configEditor.setCustomEditor("ourCallsign", new CallsignCustomField())
  private val contestTypeProperty: ObjectProperty[ContestType] = configEditor.getProperty("contestType")
  configEditor.setCustomEditor("ourClass", contestCatalog.comboBox(contestTypeProperty))
  configEditor.setCustomEditor("ourSection", new SectionComboBox(sectionsProvider))
  configEditor.hideField("stamp")

  private val configPane = configEditor.horizontal
  private val currentContestConfigProperty: ReadOnlyObjectProperty[ContestConfig] = configEditor.currentValueProperty

  title = "Contest Configuration"
  resizable = true
  dialogPane().content = new VBox(spacing = 8):
    children ++= Seq(configPane, exchangePane.pane(currentContestConfigProperty))

  dialogPane().buttonTypes = Seq(ButtonType.OK, ButtonType.Cancel)

  private val updateButton = dialogPane().lookupButton(ButtonType.OK)
  configEditor.onAnyFieldChange(
    () => updateButton.setDisable(!configEditor.isValid)
  )
  updateButton.setDisable(!configEditor.isValid)
  updateButton.addEventFilter(
    javafx.event.ActionEvent.ACTION,
    (_: javafx.event.ActionEvent) =>
      contestManager.setConfig(
        configEditor.finish().copy(
          stamp = Instant.now()
        )
      )
  )
