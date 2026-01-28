package fdswarm.fx.caseForm

import scalafx.scene.control.{Control, TextField, CheckBox, ComboBox}
import scalafx.collections.ObservableBuffer

trait FieldControl[A]:
  type C <: Control
  def create(name: String, initial: Option[A]): C
  def getValue(control: C): A

object FieldControl:
  // You can keep Aux for *type signatures* if you like,
  // just don't use it on the left-hand side of `given ... with`.
  type Aux[A, CC <: Control] = FieldControl[A] { type C = CC }

  given stringField: FieldControl[String] with
    type C = TextField

    def create(name: String, initial: Option[String]): TextField =
      val tf = new TextField()
      tf.text = initial.getOrElse("")
      tf

    def getValue(control: TextField): String =
      control.text.value

  given intField: FieldControl[Int] with
    type C = TextField

    def create(name: String, initial: Option[Int]): TextField =
      val tf = new TextField()
      tf.text = initial.map(_.toString).getOrElse("")
      tf

    def getValue(control: TextField): Int =
      val s = control.text.value.trim
      if s.isEmpty then 0 else s.toInt

  given booleanField: FieldControl[Boolean] with
    type C = CheckBox

    def create(name: String, initial: Option[Boolean]): CheckBox =
      val cb = new CheckBox()
      cb.selected = initial.getOrElse(false)
      cb

    def getValue(control: CheckBox): Boolean =
      control.selected.value

  // ---------- Enum support ----------
  import scala.deriving.Mirror
  import scala.compiletime.{erasedValue, summonInline}

  // top-level inline recursion (NOT nested inside the given)
  private inline def enumValuesFromTuple[E, Cases <: Tuple]: List[E] =
    inline erasedValue[Cases] match
      case _: EmptyTuple => Nil
      case _: (h *: t) =>
        summonInline[ValueOf[h]].value.asInstanceOf[E] ::
          enumValuesFromTuple[E, t]

  inline given enumField[E](using m: Mirror.SumOf[E]): FieldControl[E] with
    type C = ComboBox[E]

    def create(name: String, initial: Option[E]): ComboBox[E] =
      val values: List[E] = enumValuesFromTuple[E, m.MirroredElemTypes]
      val cb = new ComboBox[E](ObservableBuffer.from(values))
      initial.foreach(cb.selectionModel().select)
      cb

    def getValue(control: ComboBox[E]): E =
      control.selectionModel().getSelectedItem