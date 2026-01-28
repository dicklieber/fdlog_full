/*
 * Copyright (c) 2026. Dick Lieber, WA9NNN
 *
 * This program is free software: you can redistribute it and/or modify 
 * it under the terms of the GNU General Public License as published by 
 * the Free Software Foundation, either version 3 of the License, or    
 * (at your option) any later version.                                  
 *                                                                      
 * This program is distributed in the hope that it will be useful,      
 * but WITHOUT ANY WARRANTY; without even the implied warranty of       
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the        
 * GNU General Public License for more details.                         
 *                                                                      
 * You should have received a copy of the GNU General Public License    
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package fdswarm.fx.caseForm

import fdswarm.fx.caseForm.FieldControl
import scalafx.scene.control.{Control, Label}

import scala.compiletime.{constValueTuple, erasedValue, summonInline}
import scala.deriving.Mirror

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
    if errs.nonEmpty then Left(errs)
    else Right(buildUnsafe())

object CaseForm:

  inline def apply[T](
                       initial: Option[T] = None,
                       validations: Map[String, Any => List[String]] = Map.empty
                     )(using m: Mirror.ProductOf[T]): CaseForm[T] =
    type Elems = m.MirroredElemTypes
    type Labels = m.MirroredElemLabels

    val labels: List[String] =
      constValueTuple[Labels].toList.asInstanceOf[List[String]]

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
          f.control.style =
            if msgs.nonEmpty then "-fx-border-color: red;"
            else ""

          if msgs.nonEmpty then Some(FieldError(f.name, msgs))
          else None
      }.toList

    new CaseForm[T](fieldsVec, byName, buildT, validateAll)

  private inline def buildFields[Elems <: Tuple](
                                                  labels: List[String],
                                                  initial: Option[Elems],
                                                  validations: Map[String, Any => List[String]]
                                                ): Vector[Field[?]] =
    inline erasedValue[Elems] match
      case _: EmptyTuple =>
        Vector.empty
      case _: (head *: tail) =>
        val nameHead :: labelTail =
          labels match
            case h :: t => h :: t
            case Nil => throw new IllegalStateException("Not enough labels")

        val headInitial: Option[head] =
          initial.map(_.head).asInstanceOf[Option[head]]

        val headField: Field[head] =
          makeField[head](nameHead, headInitial, validations)

        val tailInitial: Option[tail] =
          initial.map(_.tail.asInstanceOf[tail])

        headField +: buildFields[tail](labelTail, tailInitial, validations)

  private inline def makeField[A](
                                   name: String,
                                   initial: Option[A],
                                   validations: Map[String, Any => List[String]]
                                 ): Field[A] =
    val tc = summonInline[FieldControl[A]]
    val c = tc.create(name, initial)
    val lbl = new Label()

    val validatorAny: Any => List[String] =
      validations.getOrElse(name, (_: Any) => Nil)

    val validatorA: A => List[String] =
      (a: A) => validatorAny(a)

    Field[A](
      name = name,
      control = c, //
      errorLabel = lbl,
      getValue = () => tc.getValue(c.asInstanceOf[tc.C]),
      validateValue = validatorA
    )

  private inline def tupleFromFields[Elems <: Tuple](it: Iterator[Field[?]]): Elems =
    inline erasedValue[Elems] match
      case _: EmptyTuple =>
        EmptyTuple.asInstanceOf[Elems]
      case _: (head *: tail) =>
        val fHead = it.next().asInstanceOf[Field[head]]
        val vHead: head = fHead.getValue()
        val vTail: tail = tupleFromFields[tail](it)
        (vHead *: vTail).asInstanceOf[Elems]

  private inline def productToTuple[Elems <: Tuple](p: Product): Elems =
    tupleFromProduct[Elems](p, 0)

  private inline def tupleFromProduct[Elems <: Tuple](p: Product, idx: Int): Elems =
    inline erasedValue[Elems] match
      case _: EmptyTuple =>
        EmptyTuple.asInstanceOf[Elems]
      case _: (head *: tail) =>
        val headVal = p.productElement(idx).asInstanceOf[head]
        val tailVal: tail = tupleFromProduct[tail](p, idx + 1)
        (headVal *: tailVal).asInstanceOf[Elems]

  final case class Field[A](
                             name: String,
                             control: Control,
                             errorLabel: Label,
                             getValue: () => A,
                             validateValue: A => List[String]
                           )