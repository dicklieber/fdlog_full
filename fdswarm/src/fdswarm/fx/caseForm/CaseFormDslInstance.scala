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

import fdswarm.fx.caseForm.CaseForm
import scalafx.scene.control.{Control, Label}

import scala.deriving.Mirror

final class CaseFormDslInstance[T](
                                    val form: CaseForm[T],
                                    private val formValidations: List[T => List[String]]
                                  ):
  def fields = form.fields
  def control(name: String): Option[Control] = form.control(name)
  def errorLabel(name: String): Option[Label] = form.errorLabel(name)

  /** Field validations already drawn onto the UI by CaseForm.valueEither.
   * This adds form-level validations.
   */
  def valueEither: Either[List[String], T] =
    form.valueEither match
      case Left(fieldErrs) =>
        Left(fieldErrs.flatMap(e => e.messages.map(m => s"${e.field}: $m")))
      case Right(t) =>
        val formMsgs = formValidations.flatMap(_(t))
        if formMsgs.nonEmpty then Left(formMsgs) else Right(t)

final class CaseFormBuilder[T] private[fx] (
                                             private val initial: Option[T],
                                             private val fieldValidations: Map[String, Any => List[String]],
                                             private val formValidations: List[T => List[String]]
                                           )(using Mirror.ProductOf[T]):

  def withInitial(value: T): CaseFormBuilder[T] =
    new CaseFormBuilder(Some(value), fieldValidations, formValidations)

  def verifyingField(fieldName: String)(f: Any => List[String]): CaseFormBuilder[T] =
    new CaseFormBuilder(initial, fieldValidations + (fieldName -> f), formValidations)

  def verifyingFieldSimple[A](fieldName: String)(cond: A => Boolean, msg: String): CaseFormBuilder[T] =
    verifyingField(fieldName) { any =>
      val a = any.asInstanceOf[A]
      if cond(a) then Nil else List(msg)
    }

  def verifying(msg: String)(cond: T => Boolean): CaseFormBuilder[T] =
    val f: T => List[String] = t => if cond(t) then Nil else List(msg)
    new CaseFormBuilder(initial, fieldValidations, formValidations :+ f)

  def build(): CaseFormDslInstance[T] =
    val baseForm = CaseForm[T](initial, fieldValidations)
    new CaseFormDslInstance[T](baseForm, formValidations)

object CaseFormBuilder:
  def apply[T](using Mirror.ProductOf[T]): CaseFormBuilder[T] =
    new CaseFormBuilder[T](None, Map.empty, Nil)

// nicer name
object CaseFormDsl:
  def apply[T](using Mirror.ProductOf[T]): CaseFormBuilder[T] =
    CaseFormBuilder[T]