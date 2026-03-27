package fdswarm.fx.utils

import scalafx.Includes.*
import scalafx.beans.property.*
import scalafx.collections.ObservableBuffer
import scalafx.scene.Node
import scalafx.scene.control.*
import scalafx.scene.layout.*

import java.lang.reflect.Constructor
import scala.collection.mutable

class CaseClassPropertyEditor[T <: Product](
                                             val target: ObjectProperty[T]
                                           ) extends VBox:

  require(target != null, "target must not be null")
  require(target.value != null, "target.value must not be null")

  private def parseIntValue(s: String): Option[Int] =
    try Some(s.trim.toInt)
    catch
      case _: NumberFormatException => None

  private def parseLongValue(s: String): Option[Long] =
    try Some(s.trim.toLong)
    catch
      case _: NumberFormatException => None

  private def parseDoubleValue(s: String): Option[Double] =
    try Some(s.trim.toDouble)
    catch
      case _: NumberFormatException => None

  sealed trait FieldValue:
    def value: Any
    def value_=(v: Any): Unit
    def defaultEditor: Node

  final class StringField(val property: StringProperty) extends FieldValue:
    def value: Any =
      property.value

    def value_=(v: Any): Unit =
      property.value = v.asInstanceOf[String]

    def defaultEditor: Node =
      new TextField:
        text <==> property

  final class IntField(val property: IntegerProperty) extends FieldValue:
    def value: Any =
      property.value

    def value_=(v: Any): Unit =
      property.value = v.asInstanceOf[Int]

    def defaultEditor: Node =
      new TextField:
        text = property.value.toString
        text.onChange { (_, _, nv) =>
          parseIntValue(nv).foreach(v => property.value = v)
        }

  final class LongField(val property: LongProperty) extends FieldValue:
    def value: Any =
      property.value

    def value_=(v: Any): Unit =
      property.value = v.asInstanceOf[Long]

    def defaultEditor: Node =
      new TextField:
        text = property.value.toString
        text.onChange { (_, _, nv) =>
          parseLongValue(nv).foreach(v => property.value = v)
        }

  final class DoubleField(val property: DoubleProperty) extends FieldValue:
    def value: Any =
      property.value

    def value_=(v: Any): Unit =
      property.value = v.asInstanceOf[Double]

    def defaultEditor: Node =
      new TextField:
        text = property.value.toString
        text.onChange { (_, _, nv) =>
          parseDoubleValue(nv).foreach(v => property.value = v)
        }

  final class BooleanField(val property: BooleanProperty) extends FieldValue:
    def value: Any =
      property.value

    def value_=(v: Any): Unit =
      property.value = v.asInstanceOf[Boolean]

    def defaultEditor: Node =
      new CheckBox:
        selected <==> property

  final class ObjectField(val property: ObjectProperty[Any]) extends FieldValue:
    def value: Any =
      property.value

    def value_=(v: Any): Unit =
      property.value = v

    def defaultEditor: Node =
      new TextField:
        text = Option(property.value).map(_.toString).getOrElse("")
        disable = true
        promptText = "Unsupported type for direct editing"

  private val customEditors =
    mutable.LinkedHashMap.empty[String, () => Node]

  private var updatingFromTarget = false

  private val initialValue: T =
    target.value

  private var currentClass: Class[?] =
    initialValue.getClass

  val fieldNames: Vector[String] =
    initialValue.productElementNames.toVector

  val fields: Map[String, FieldValue] =
    buildFields(initialValue)

  def field(name: String): FieldValue =
    fields.getOrElse(
      name,
      throw new NoSuchElementException(s"No field named '$name'")
    )

  def stringProperty(name: String): StringProperty =
    field(name) match
      case f: StringField => f.property
      case _ => throw new IllegalArgumentException(s"Field '$name' is not a String")

  def intProperty(name: String): IntegerProperty =
    field(name) match
      case f: IntField => f.property
      case _ => throw new IllegalArgumentException(s"Field '$name' is not an Int")

  def longProperty(name: String): LongProperty =
    field(name) match
      case f: LongField => f.property
      case _ => throw new IllegalArgumentException(s"Field '$name' is not a Long")

  def doubleProperty(name: String): DoubleProperty =
    field(name) match
      case f: DoubleField => f.property
      case _ => throw new IllegalArgumentException(s"Field '$name' is not a Double")

  def booleanProperty(name: String): BooleanProperty =
    field(name) match
      case f: BooleanField => f.property
      case _ => throw new IllegalArgumentException(s"Field '$name' is not a Boolean")

  def objectProperty(name: String): ObjectProperty[Any] =
    field(name) match
      case f: ObjectField => f.property
      case _ => throw new IllegalArgumentException(s"Field '$name' is not an Object field")

  def objectPropertyAs[A](name: String): ObjectProperty[A] =
    objectProperty(name).asInstanceOf[ObjectProperty[A]]

  def setCustomEditor(name: String)(builder: => Node): Unit =
    require(fieldNames.contains(name), s"No field named '$name'")
    customEditors(name) = () => builder

  def clearCustomEditor(name: String): Unit =
    customEditors.remove(name)

  def hasCustomEditor(name: String): Boolean =
    customEditors.contains(name)

  def defaultEditorFor(name: String): Node =
    field(name).defaultEditor

  def controlFor(name: String): Node =
    customEditors.get(name) match
      case Some(builder) => builder()
      case None          => defaultEditorFor(name)

  def labelFor(name: String): Label =
    new Label(name)

  def save(): Unit =
    val rebuilt =
      rebuild(
        clazz = currentClass,
        fieldNames = fieldNames,
        fields = fields
      ).asInstanceOf[T]

    updatingFromTarget = true
    try
      target.value = rebuilt
    finally
      updatingFromTarget = false

  val saveButton: Button =
    new Button("Save"):
      onAction = _ => save()

  val form: GridPane =
    buildForm()

  def formExcluding(excluded: Set[String]): GridPane =
    buildForm(
      excluded = excluded,
      included = fieldNames
    )

  def formIncluding(included: Seq[String]): GridPane =
    buildForm(
      excluded = Set.empty,
      included = included
    )

  def horizontalFormExcluding(excluded: Set[String]): GridPane =
    buildHorizontalForm(
      excluded = excluded,
      included = fieldNames
    )

  def horizontalFormIncluding(included: Seq[String]): GridPane =
    buildHorizontalForm(
      excluded = Set.empty,
      included = included
    )

  children = Seq(form, saveButton)

  target.onChange { (_, _, newValue) =>
    if !updatingFromTarget && newValue != null then
      syncFromTarget(newValue)
  }

  private def buildForm(
                         excluded: Set[String] = Set.empty,
                         included: Seq[String] = fieldNames
                       ): GridPane =
    val shownNames =
      included.filter(name => fieldNames.contains(name) && !excluded.contains(name))

    new GridPane:
      hgap = 8
      vgap = 8

      for (name, row) <- shownNames.zipWithIndex do
        add(labelFor(name), 0, row)
        add(controlFor(name), 1, row)

  private def buildHorizontalForm(
                                   excluded: Set[String] = Set.empty,
                                   included: Seq[String] = fieldNames
                                 ): GridPane =
    val shownNames =
      included.filter(name => fieldNames.contains(name) && !excluded.contains(name))

    new GridPane:
      hgap = 12
      vgap = 8

      for (name, col) <- shownNames.zipWithIndex do
        add(labelFor(name), col, 0)
        add(controlFor(name), col, 1)

  private def buildFields(value: T): Map[String, FieldValue] =
    val values = value.productIterator.toVector
    fieldNames.zip(values).map { (name, fieldValue) =>
      name -> newField(fieldValue)
    }.toMap

  private def newField(value: Any): FieldValue =
    value match
      case v: String  => new StringField(StringProperty(v))
      case v: Int     => new IntField(IntegerProperty(v))
      case v: Long    => new LongField(LongProperty(v))
      case v: Double  => new DoubleField(DoubleProperty(v))
      case v: Boolean => new BooleanField(BooleanProperty(v))
      case v          => new ObjectField(ObjectProperty[Any](v))

  private def syncFromTarget(value: T): Unit =
    val newClass = value.getClass
    require(
      newClass == currentClass,
      s"CaseClassPropertyEditor does not support changing runtime class from ${currentClass.getName} to ${newClass.getName}"
    )

    val values = value.productIterator.toVector

    for (name, fieldValue) <- fieldNames.zip(values) do
      fields(name).value = fieldValue

  private def rebuild(
                       clazz: Class[?],
                       fieldNames: Seq[String],
                       fields: Map[String, FieldValue]
                     ): Any =
    val ctor = primaryConstructor(clazz)
    val args = fieldNames.map { name =>
      fields
        .getOrElse(
          name,
          throw new IllegalArgumentException(s"Missing field for '$name'")
        )
        .value
        .asInstanceOf[Object]
    }
    ctor.newInstance(args*)

  private def primaryConstructor(clazz: Class[?]): Constructor[?] =
    val ctor = clazz.getDeclaredConstructors.maxBy(_.getParameterCount)
    ctor.setAccessible(true)
    ctor