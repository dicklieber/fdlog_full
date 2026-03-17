package fdswarm.fx.utils

import scalafx.scene.control.Button
import scalafx.scene.paint.Color

object IconButton:

  def apply(
      name: String,
      size: Double = 16,
      tooltipText: String = "",
      color: Color = Color.Black
  ): Button =

    val icon = BootstrapIcons.svgPath(name, size, color)

    val btn = new Button:
      graphic = icon
      style =
        """
          -fx-background-color: white;
          -fx-border-color: transparent;
          -fx-padding: 6;
        """

    if tooltipText.nonEmpty then
      btn.tooltip = tooltipText

    btn.onMouseEntered = _ =>
      btn.style =
        """
          -fx-background-color: rgba(0,0,0,0.08);
          -fx-border-color: transparent;
          -fx-padding: 6;
        """

    btn.onMouseExited = _ =>
      btn.style =
        """
          -fx-background-color: transparent;
          -fx-border-color: transparent;
          -fx-padding: 6;
        """

    btn
