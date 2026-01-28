package fdswarm.fx.caseForm

import scala.deriving.Mirror
import scala.compiletime.{erasedValue, constValueTuple, summonInline}
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

  inline def apply[T](
    initial: Option[T] = None,
    validations: Map[String, Any => List[String]] = Map.empty
  )(using m: Mirror.ProductOf[T]): CaseForm[T] =
    type Labels = m.MirroredElemLabels
    val labels = constValueTuple[Labels].toList.asInstanceOf[List[String]]
    fromLabels[T](labels, initial, validations)

  inline def fromLabels[T](
    labels: List[String],
    initial: Option[T],
    validations: Map[String, Any => List[String]]
  )(using m: Mirror.ProductOf[T]): CaseForm[T] =
    type Elems = m.MirroredElemTypes

    val initialTupleOpt: Option[Elems] =
      initial.map(t => productToTuple[Elems](t.asInstanceOf[Product]))

    val fieldsVec: Vector[Field[?]] =
      buildFields[Elems](labels, initialTupleOpt, validations)

    val byName: Map[String, Field[?]] =
      fieldsVec.map(f => f.name -> f).toMap

    def buildT(): T =
      val elems: Elems = tupleFromFields[Elems](fieldsVec.iterator)
      m.fromProduct(elems)

    def validateAll(): List[FieldError] =
      fieldsVec.flatMap {
        case f: Field[a] =>
          val v: a = f.getValue()
          val msgs = f.validateValue(v)
          f.errorLabel.text = msgs.mkString("\n")
          f.control.style = if msgs.nonEmpty then "-fx-border-color: red;" else ""
          if msgs.nonEmpty then Some(FieldError(f.name, msgs)) else None
      }.toList

    new CaseForm[T](fieldsVec, byName, buildT, validateAll)

  private inline def buildFields[Elems <: Tuple](
    labels: List[String],
    initial: Option[Elems],
    validations: Map[String, Any => List[String]]
  ): Vector[Field[?]] =
    inline erasedValue[Elems] match
      case _: EmptyTuple => Vector.empty
      case _: (h *: t) =>
        val (name, rest) =
          labels match
            case x :: xs => (x, xs)
            case Nil => throw new IllegalStateException("Not enough labels")

        val headInit = initial.map(_.head).asInstanceOf[Option[h]]
        val field = makeField[h](name, headInit, validations)
        val tailInit = initial.map(_.tail.asInstanceOf[t])
        field +: buildFields[t](rest, tailInit, validations)

  private inline def makeField[A](
    name: String,
    initial: Option[A],
    validations: Map[String, Any => List[String]]
  ): Field[A] =
    val tc = summonInline[FieldControl[A]]
    val c = tc.create(name, initial)
    val lbl = new Label()
    val vAny = validations.getOrElse(name, (_: Any) => Nil)

    Field(
      name,
      c.asInstanceOf[Control],
      lbl,
      () => tc.getValue(c.asInstanceOf[tc.C]),
      (a: A) => vAny(a)
    )

  private inline def tupleFromFields[Elems <: Tuple](it: Iterator[Field[?]]): Elems =
    inline erasedValue[Elems] match
      case _: EmptyTuple => EmptyTuple.asInstanceOf[Elems]
      case _: (h *: t) =>
        val f = it.next().asInstanceOf[Field[h]]
        (f.getValue() *: tupleFromFields[t](it)).asInstanceOf[Elems]

  private inline def productToTuple[Elems <: Tuple](p: Product): Elems =
    tupleFromProduct[Elems](p, 0)

  private inline def tupleFromProduct[Elems <: Tuple](p: Product, i: Int): Elems =
    inline erasedValue[Elems] match
      case _: EmptyTuple => EmptyTuple.asInstanceOf[Elems]
      case _: (h *: t) =>
        (p.productElement(i).asInstanceOf[h] *:
          tupleFromProduct[t](p, i + 1)).asInstanceOf[Elems]
