package fdswarm.fx.caseForm

import scalafx.scene.control.ComboBox

/**
 * A "field value" that knows:
 *  - the current selected value
 *  - how to build a ComboBox with the valid choices (from anywhere: store, catalog, db, etc.)
 *
 * This is meant to be embedded in your case class, like enum fields are.
 */
final case class ChoiceField[A <: AnyRef](
                                           value: A,
                                           build: Option[A] => ComboBox[A]
                                         ):
  def comboBox(): ComboBox[A] =
    build(Some(value))

  def withValue(v: A): ChoiceField[A] =
    copy(value = v)