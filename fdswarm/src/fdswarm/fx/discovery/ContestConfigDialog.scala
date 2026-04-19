package fdswarm.fx.discovery

import fdswarm.fx.contest.*
import fdswarm.fx.sections.SectionsProvider
import fdswarm.fx.utils.StyledDialog
import fdswarm.fx.utils.editor.{CallsignCustomField, CaseClassPropertyEditor, CustomFieldEditor, IntSpinner}
import jakarta.inject.Inject
import scalafx.Includes.*
import scalafx.beans.property.{ObjectProperty, ReadOnlyObjectProperty}
import scalafx.scene.Node
import scalafx.scene.control.ButtonType
import scalafx.scene.layout.VBox

import scala.jdk.CollectionConverters.*

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
  configEditor.onAnyFieldChange(() => updateButton.setDisable(hasInvalidCustomEditor(configPane)))
  updateButton.setDisable(hasInvalidCustomEditor(configPane))
  updateButton.addEventFilter(
    javafx.event.ActionEvent.ACTION,
    (_: javafx.event.ActionEvent) => contestManager.setConfig(configEditor.finish()))

  private def hasInvalidCustomEditor(root: Node): Boolean = allNodes(root.delegate).exists(node =>
    Option(node.getProperties.get(CaseClassPropertyEditor.CustomFieldEditorNodeKey)).exists {
      case editor: CustomFieldEditor =>
        !editor.isValid
      case _ =>
        false
    })

  private def allNodes(root: javafx.scene.Node): Seq[javafx.scene.Node] =
    val children = root match
      case parent: javafx.scene.Parent => parent.getChildrenUnmodifiable.asScala.toSeq
      case _                           => Seq.empty

    root +: children.flatMap(allNodes)
