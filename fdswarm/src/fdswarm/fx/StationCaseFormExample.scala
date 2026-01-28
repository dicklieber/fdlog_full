package fdswarm.fx

import fdswarm.fx.caseForm.{CaseForm, FieldControl}
import fdswarm.fx.Station.{Band, Mode}
import fdswarm.model.Qso.CallSign

import scalafx.geometry.Insets
import scalafx.scene.Node
import scalafx.scene.control.{Button, Label, TextField}
import scalafx.scene.layout.{GridPane, HBox, VBox}

// IMPORTANT: this is the ScalaFX -> JavaFX EventHandler bridge
import scalafx.Includes.eventClosureWrapperWithParam
import javafx.event.ActionEvent

import java.time.Instant

object StationCaseFormExample:

  /** Local FieldControl for Instant so CaseForm[Station] can be generated. */
  given instantFieldControl: FieldControl[Instant] with
    type C = TextField
    def create(name: String, initial: Option[Instant]): TextField =
      val tf = new TextField()
      tf.editable = false
      tf.text = initial.getOrElse(Instant.now()).toString
      tf
    def getValue(control: TextField): Instant =
      Instant.parse(control.text.value)

  private val callSignRe = "^[A-Z0-9/]{3,}$".r

  def pane(
            initial: Station = Station(),
            onSave: Station => Unit
          ): Node =

    val validations: Map[String, Any => List[String]] =
      Map(
        "bandName" -> { any =>
          val s = any.asInstanceOf[Band].trim
          if s.nonEmpty then Nil else List("Band is required")
        },
        "modeName" -> { any =>
          val s = any.asInstanceOf[Mode].trim
          if s.nonEmpty then Nil else List("Mode is required")
        },
        "operator" -> { any =>
          val s = any.asInstanceOf[CallSign].trim.toUpperCase
          if s.nonEmpty && callSignRe.matches(s) then Nil else List("Operator callsign looks invalid")
        }
      )

    val form: CaseForm[Station] =
      CaseForm[Station](initial = Some(initial), validations = validations)

    val grid = new GridPane:
      hgap = 8
      vgap = 6
      padding = Insets(10)

    def addRow(row: Int, field: String, labelText: String): Unit =
      val lbl = new Label(labelText)
      val ctl = form.control(field).getOrElse(sys.error(s"Missing control for '$field'"))
      val err = form.errorLabel(field).getOrElse(sys.error(s"Missing error label for '$field'"))
      err.wrapText = true
      err.maxWidth = 280

      grid.add(lbl, 0, row)
      grid.add(ctl, 1, row)
      grid.add(err, 2, row)

    addRow(0, "bandName", "Band")
    addRow(1, "modeName", "Mode")
    addRow(2, "operator", "Operator")
    addRow(3, "rig", "Rig")
    addRow(4, "antenna", "Antenna")
    addRow(5, "stamp", "Stamp") // read-only Instant field

    val status = new Label("")
    val save = new Button("Save")

    save.onAction = (_: ActionEvent) =>
      form.valueEither match
        case Left(fieldErrs) =>
          status.text =
            fieldErrs
              .map(e => s"${e.field}: ${e.messages.mkString(", ")}")
              .mkString(" • ")

        case Right(stationFromControls) =>
          val station = stationFromControls.copy(stamp = Instant.now())
          onSave(station)

    new VBox:
      spacing = 10
      padding = Insets(10)
      children = Seq(
        grid,
        new HBox:
          spacing = 10
          children = Seq(save, status)
      )