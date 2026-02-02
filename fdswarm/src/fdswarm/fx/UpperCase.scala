package fdswarm.fx

import scalafx.scene.control.TextField

object UpperCase:

  def apply(tf: TextField): TextField =
    tf.text.onChange { (_, _, newValue) =>
      if newValue != null then
        val upper = newValue.toUpperCase
        if upper != newValue then
          tf.text = upper
    }
    tf