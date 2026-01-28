package fdswarm.fx.caseForm

import scala.deriving.Mirror
import scala.compiletime.constValueTuple
import scalafx.scene.control.{Control, Label}

final class CaseFormDslInstance[T](
  val form: CaseForm[T],
  private val formValidations: List[T => List[String]]
):
  def control(name: String): Option[Control] = form.control(name)
  def errorLabel(name: String): Option[Label] = form.errorLabel(name)

  def valueEither: Either[List[String], T] =
    form.valueEither match
      case Left(errs) =>
        Left(errs.flatMap(e => e.messages.map(m => s"${e.field}: $m")))
      case Right(t) =>
        val msgs = formValidations.flatMap(_(t))
        if msgs.nonEmpty then Left(msgs) else Right(t)

final class CaseFormBuilder[T](
  private val labels: List[String],
  private val initial: Option[T],
  private val fieldValidations: Map[String, Any => List[String]],
  private val formValidations: List[T => List[String]]
)(using Mirror.ProductOf[T]):

  def withInitial(value: T): CaseFormBuilder[T] =
    new CaseFormBuilder(labels, Some(value), fieldValidations, formValidations)

  def verifyingField(field: String)(f: Any => List[String]): CaseFormBuilder[T] =
    new CaseFormBuilder(labels, initial, fieldValidations + (field -> f), formValidations)

  def verifyingFieldSimple[A](field: String)(cond: A => Boolean, msg: String): CaseFormBuilder[T] =
    verifyingField(field) { any =>
      val a = any.asInstanceOf[A]
      if cond(a) then Nil else List(msg)
    }

  def verifying(msg: String)(cond: T => Boolean): CaseFormBuilder[T] =
    new CaseFormBuilder(
      labels,
      initial,
      fieldValidations,
      formValidations :+ (t => if cond(t) then Nil else List(msg))
    )

  inline def build(): CaseFormDslInstance[T] =
    val base = CaseForm.fromLabels[T](labels, initial, fieldValidations)
    new CaseFormDslInstance[T](base, formValidations)

object CaseFormBuilder:
  inline def apply[T](using m: Mirror.ProductOf[T]): CaseFormBuilder[T] =
    type Labels = m.MirroredElemLabels
    val labels = constValueTuple[Labels].toList.asInstanceOf[List[String]]
    new CaseFormBuilder[T](labels, None, Map.empty, Nil)

object CaseFormDsl:
  inline def apply[T](using m: Mirror.ProductOf[T]): CaseFormBuilder[T] =
    CaseFormBuilder[T]
