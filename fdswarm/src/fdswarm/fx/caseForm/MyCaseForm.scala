package fdswarm.fx.caseForm

import fdswarm.fx.InputHelper.*
import fdswarm.model.Callsign
import javafx.event.{ActionEvent as JfxActionEvent, EventHandler as JfxEventHandler}
import scalafx.Includes.*
import scalafx.collections.ObservableBuffer
import scalafx.geometry.Insets
import scalafx.scene.Node
import scalafx.scene.control.*
import scalafx.scene.layout.{GridPane, HBox, Pane, VBox}

import java.time.{Instant, LocalTime, ZoneId, ZonedDateTime}
import scala.deriving.Mirror

class MyCaseForm[T <: Product](initial: T, onSave: T => Unit)(using m: Mirror.ProductOf[T]):

  /** For Scala 3 enums that are *not* Java enums at runtime (e.g. `enum HamBand(val ...)`). */
  private def scala3EnumItems(value: Any): Option[Seq[AnyRef]] =
    // Example runtime class name for a case: fdswarm.model.HamBand$B12m$
    val n = value.getClass.getName
    val dollar = n.indexOf('$')
    if dollar < 0 then None
    else
      val enumTypeName  = n.substring(0, dollar) // -> fdswarm.model.HamBand
      val companionName = enumTypeName + "$"     // -> fdswarm.model.HamBand$

      try
        val companionClass = Class.forName(companionName)
        val module         = companionClass.getField("MODULE$").get(null)
        val valuesMethod   = companionClass.getMethod("values")
        val values         = valuesMethod.invoke(module).asInstanceOf[Array[?]]
        Some(values.iterator.map(_.asInstanceOf[AnyRef]).toSeq)
      catch
        case _: ReflectiveOperationException =>
          None

  /** For Java enums (and Scala 3 “simple” enums that compile to Java enums). */
  private def javaEnumItems(e: java.lang.Enum[?]): Seq[AnyRef] =
    val constants = e.getDeclaringClass.getEnumConstants
    constants.iterator.map(_.asInstanceOf[AnyRef]).toSeq

  val fields: IndexedSeq[Field[Any]] =
    for
      i <- 0 until initial.productArity
      fieldValue = initial.productElement(i)
      if !fieldValue.isInstanceOf[Instant] && !fieldValue.isInstanceOf[fdswarm.ContestDates] // keep Instants and ContestDates as-is
    yield
      val (control, getter): (Node, () => Any) =
        fieldValue match
          case zdt: ZonedDateTime =>
            val datePicker = new DatePicker(zdt.toLocalDate)
            val hourSpinner = new Spinner[Int](0, 23, zdt.getHour)
            hourSpinner.prefWidth = 60
            val minSpinner = new Spinner[Int](0, 59, zdt.getMinute)
            minSpinner.prefWidth = 60
            val hb = new HBox(5, datePicker, new Label("H:"), hourSpinner, new Label("M:"), minSpinner)
            (hb, () => ZonedDateTime.of(datePicker.value.value, LocalTime.of(hourSpinner.value.value, minSpinner.value.value), zdt.getZone))

          // ---- Provided-choice support (e.g. HamBand from AvailableBandsStore) ----
          case cf: ChoiceField[?] =>
            val cf0   = cf.asInstanceOf[ChoiceField[AnyRef]]
            val combo = cf0.comboBox().asInstanceOf[ComboBox[AnyRef]]
            (combo, () => cf0.withValue(combo.value.value))

          case _ =>
            // First try Scala 3 parameterized enum detection (HamBand, etc.)
            scala3EnumItems(fieldValue) match
              case Some(items) =>
                val combo = new ComboBox[AnyRef](ObservableBuffer.from(items))
                combo.value.value = fieldValue.asInstanceOf[AnyRef]
                (combo, () => combo.value.value)

              case None =>
                fieldValue match
                  case cs: Callsign =>
                    val tf = new TextField:
                      text = cs.value
                      promptText = "Enter callsign"
                    forceCaps(tf)
                    (tf, () => Callsign(tf.text.value.trim))

                  case e: java.lang.Enum[_] =>
                    val items = javaEnumItems(e)
                    val combo = new ComboBox[AnyRef](ObservableBuffer.from(items))
                    combo.value.value = e.asInstanceOf[AnyRef]
                    (combo, () => combo.value.value)

                  case cf: ChoiceField[?] =>
                    val cf0   = cf.asInstanceOf[ChoiceField[AnyRef]]
                    val combo = cf0.comboBox().asInstanceOf[ComboBox[AnyRef]]
                    (combo, () => cf0.withValue(combo.value.value))
                  case b: Boolean =>
                    val cb = new CheckBox:
                      selected = b
                    (cb, () => cb.selected.value)

                  case s: String =>
                    val tf = new TextField:
                      text = s
                    (tf, () => tf.text.value)

                  case i0: Int =>
                    val tf = new TextField:
                      text = i0.toString
                    (tf, () => tf.text.value.trim.toInt)

                  case l0: Long =>
                    val tf = new TextField:
                      text = l0.toString
                    (tf, () => tf.text.value.trim.toLong)

                  case d0: Double =>
                    val tf = new TextField:
                      text = d0.toString
                    (tf, () => tf.text.value.trim.toDouble)

                  case other =>
                    throw new IllegalArgumentException(
                      s"Unsupported field type: $other (${other.getClass.getName})"
                    )

      val name = initial.productElementName(i)
      Field(name, control, new Label(name), getter)

  val saveButton = new Button("Save")
  saveButton.onAction = new JfxEventHandler[JfxActionEvent]:
    override def handle(e: JfxActionEvent): Unit =
      onSave(result)

  def pane(): Pane =
    val grid = new GridPane:
      hgap = 8
      vgap = 6
      padding = Insets(10)

    fields.zipWithIndex.foreach { case (field, index) =>
      grid.add(new Label(field.name), 0, index)
      grid.add(field.control, 1, index)
    }

    new VBox(grid, saveButton)

  def result: T =
    val values: Array[Any] = new Array[Any](initial.productArity)

    for i <- 0 until initial.productArity do
      initial.productElement(i) match
        case inst: Instant =>
          values(i) = inst // keep original Instants
        case cd: fdswarm.ContestDates =>
          values(i) = cd
        case _ =>
          val name  = initial.productElementName(i)
          val field = fields.find(_.name == name).get
          values(i) = field.getValue()

    m.fromProduct(Tuple.fromArray(values))

final case class Field[A](
                           name: String,
                           control: Node,
                           errorLabel: Label,
                           getValue: () => A
                         )