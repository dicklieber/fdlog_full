package fdswarm.fx.caseForm

import scalafx.scene.control.{Control, TextField, CheckBox, ComboBox}
import scalafx.collections.ObservableBuffer
import scala.reflect.ClassTag

trait FieldControl[A]:
  type C <: Control
  def create(name: String, initial: Option[A]): C
  def getValue(control: C): A

object FieldControl:

  given stringField: FieldControl[String] with
    type C = TextField
    def create(name: String, initial: Option[String]): TextField =
      val tf = new TextField()
      tf.text = initial.getOrElse("")
      tf
    def getValue(control: TextField): String = control.text.value

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
    def getValue(control: CheckBox): Boolean = control.selected.value

  given enumField[E <: Enum[E]](using ct: ClassTag[E]): FieldControl[E] with
    type C = ComboBox[E]
    def create(name: String, initial: Option[E]): ComboBox[E] =
      val cls = ct.runtimeClass.asInstanceOf[Class[E]]
      val values = cls.getEnumConstants.toList
      val cb = new ComboBox[E](ObservableBuffer.from(values))
      initial.foreach(cb.selectionModel().select)
      cb
    def getValue(control: ComboBox[E]): E =
      control.selectionModel().getSelectedItem
