package fdswarm.fx.caseForm

import scalafx.Includes.*
import scalafx.event.ActionEvent
import scalafx.geometry.Insets
import scalafx.scene.control.{Button, Control, Label, TextField}
import scalafx.scene.layout.{GridPane, Pane, VBox}

import java.time.Instant
import scala.deriving.Mirror

class MyCaseForm[T <: Product](initial: T)(using m: Mirror.ProductOf[T]):

  val fields: IndexedSeq[Field[String]] =
    for
      i <- 0 until initial.productArity
      value = initial.productElement(i)
      if !value.isInstanceOf[Instant]
    yield
      val tf = new TextField {
        text = value.asInstanceOf[String]
        promptText = "Enter something"
      }
      val name = initial.productElementName(i)
      Field(name, tf, new Label(name), () => tf.text.value) // ✅ String, not StringProperty

  val pane: Pane =
    val grid = new GridPane:
      hgap = 8
      vgap = 6
      padding = Insets(10)

    fields
      .zipWithIndex
      .foreach { case (field, index) =>
        val lbl = new Label(field.name)
        grid.add(lbl, 0, index)
        grid.add(field.control, 1, index)
      }

    val save = new Button("Save")
    save.onAction = (event: ActionEvent) =>
      val newData = result
      println(s"Saving: $newData")

    new VBox(grid, save)

  def result: T =
    val values = new Array[Any](initial.productArity)

    for
      i <- 0 until initial.productArity
    do
      initial.productElement(i) match
        case inst: Instant =>
          values(i) = inst // keep original Instants
        case _ =>
          val name = initial.productElementName(i)
          val field = fields.find(_.name == name).get
          values(i) = field.getValue() // ✅ now a String

    m.fromProduct(Tuple.fromArray(values))

final case class Field[A](
                           name: String,
                           control: Control,
                           errorLabel: Label,
                           getValue: () => A
                         )