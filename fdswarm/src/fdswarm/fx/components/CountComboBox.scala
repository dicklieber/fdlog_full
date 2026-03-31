package fdswarm.fx.components

import fdswarm.model.Choice

/**
 * Used for nTransmitters in Qso Search.
 */
class CountComboBox
  extends AnyComboBox[String](
    (1 to 10).map { i =>
      new Choice[String]:
        val value: String = i.toString
        val label: String = i.toString
    } :+ new Choice[String]:
      val value: String = ">10"
      val label: String = ">10"
  ):

  def check(candidate: Int): Boolean =
    value.value match
      case None =>
        true
      case Some(v) if v == ">10" =>
        candidate > 10
      case Some(v) =>
        // safer than raw toInt
        v.toIntOption match
          case Some(n) => n == candidate
          case None    => false