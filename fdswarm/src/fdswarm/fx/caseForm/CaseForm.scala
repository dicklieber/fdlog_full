package fdswarm.fx.caseForm

import scala.deriving.Mirror
import scalafx.scene.control.{Control, Label}

final case class FieldError(field: String, messages: List[String])

final class CaseForm[T](
  val fields: Vector[CaseForm.Field[?]],
  private val byName: Map[String, CaseForm.Field[?]],
  private val buildUnsafe: () => T,
  private val validateFields: () => List[FieldError]
):
  def control(name: String): Option[Control] =
    byName.get(name).map(_.control)

  def errorLabel(name: String): Option[Label] =
    byName.get(name).map(_.errorLabel)

  def valueEither: Either[List[FieldError], T] =
    val errs = validateFields()
    if errs.nonEmpty then Left(errs) else Right(buildUnsafe())

object CaseForm:

  final case class Field[A](
    name: String,
    control: Control,
    errorLabel: Label,
    getValue: () => A,
    validateValue: A => List[String]
  )

  def apply[T](
    labels: List[String],
    controlsByName: Map[String, FieldControl[?]],
    initial: Option[T] = None,
    validations: Map[String, Any => List[String]] = Map.empty
  )(using m: Mirror.ProductOf[T]): CaseForm[T] =
    fromLabels[T](labels, controlsByName, initial, validations)

  def fromLabels[T](
    labels: List[String],
    controlsByName: Map[String, FieldControl[?]],
    initial: Option[T],
    validations: Map[String, Any => List[String]]
  )(using m: Mirror.ProductOf[T]): CaseForm[T] =

    val initialProduct: Option[Product] = initial.map(_.asInstanceOf[Product])

    val fieldsVec: Vector[Field[?]] =
      labels.zipWithIndex.map { (name, idx) =>
        val initAny: Option[Any] = initialProduct.map(_.productElement(idx))
        val tcAny = controlsByName.getOrElse(
          name,
          throw new IllegalArgumentException(s"No FieldControl registered for field '$name'")
        ).asInstanceOf[FieldControl[Any]]
        makeFieldAny(name, initAny, validations, tcAny)
      }.toVector

    val byName: Map[String, Field[?]] =
      fieldsVec.map(f => f.name -> f).toMap

    def buildT(): T =
      val values: Array[Any] = fieldsVec.map {
        case f: Field[a] => f.getValue().asInstanceOf[Any]
      }.toArray

      val prod: Product = new Product {
        def productArity: Int = values.length
        def productElement(n: Int): Any = values(n)
        override def canEqual(that: Any): Boolean = true
      }

      m.fromProduct(prod)

    def validateAll(): List[FieldError] =
      fieldsVec.flatMap {
        case f: Field[a] =>
          val v: Any = f.getValue().asInstanceOf[Any]
          val msgs = f.validateValue(v.asInstanceOf[a])
          f.errorLabel.text = msgs.mkString("\n")
          f.control.style = if msgs.nonEmpty then "-fx-border-color: red;" else ""
          if msgs.nonEmpty then Some(FieldError(f.name, msgs)) else None
      }.toList

    new CaseForm[T](fieldsVec, byName, buildT, validateAll)

  private def makeFieldAny(
    name: String,
    initial: Option[Any],
    validations: Map[String, Any => List[String]],
    tc: FieldControl[Any]
  ): Field[Any] =
    val c = tc.create(name, initial)
    val lbl = new Label()
    val vAny = validations.getOrElse(name, (_: Any) => Nil)

    Field[Any](
      name,
      c.asInstanceOf[Control],
      lbl,
      () => tc.getValue(c.asInstanceOf[tc.C]),
      (a: Any) => vAny(a)
    )
