package fdswarm.fx.contest

import fdswarm.fx.utils.editor.CustomFieldEditor
import scalafx.beans.property.ObjectProperty
import scalafx.scene.Node
import scalafx.scene.control.{OverrunStyle, RadioButton, ToggleGroup}
import scalafx.scene.layout.{Region, VBox}

class ContestChooser extends CustomFieldEditor:

  override def editor(fieldProperty: Any): Node =
    fieldProperty match
      case p: ObjectProperty[?] =>
        val value = p.asInstanceOf[ObjectProperty[ContestType]]
        val tg = new ToggleGroup()

        val buttons: Seq[(ContestType, RadioButton)] =
          ContestType.values.toSeq.map { contestType =>
            val button = new RadioButton:
              text = contestType.name
              toggleGroup = tg
              minWidth = Region.USE_PREF_SIZE
              textOverrun = OverrunStyle.Clip
            contestType -> button
          }

        buttons.find(_._1 == value.value).foreach { case (_, button) =>
          button.selected = true
        }

        tg.selectedToggle.onChange { (_, _, newToggle) =>
          if newToggle != null then
            buttons.find(_._2.delegate == newToggle).foreach { case (contestType, _) =>
              if value.value != contestType then
                value.value = contestType
            }
        }

        value.onChange { (_, _, newValue) =>
          buttons.find(_._1 == newValue).foreach { case (_, button) =>
            if !button.selected.value then
              button.selected = true
          }
        }

        val box = new VBox:
          spacing = 8

        box.children ++= buttons.map(_._2)
        box

      case _ =>
        throw new IllegalArgumentException(
          s"ContestChooser requires ObjectProperty[ContestType], got ${fieldProperty.getClass.getSimpleName}"
        )