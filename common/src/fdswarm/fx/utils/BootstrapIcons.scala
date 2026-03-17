package fdswarm.fx.utils

import scalafx.scene.shape.SVGPath
import scalafx.scene.paint.Color
import scala.xml.XML

object BootstrapIcons:

  private lazy val sprite =
    XML.load(getClass.getResourceAsStream("/icons/bootstrap-icons.svg"))

  private lazy val iconMap: Map[String, String] =
    (sprite \\ "symbol").flatMap { sym =>
      val id = (sym \ "@id").text
      val path = (sym \\ "path").headOption.map(_ \@ "d")
      path.map(p => id -> p)
    }.toMap

  def svgPath(name: String, size: Double = 16, color: Color = Color.Black): SVGPath =
    val pathData =
      iconMap.getOrElse(name,
        throw new IllegalArgumentException(s"Bootstrap icon '$name' not found"))

    new SVGPath:
      content = pathData
      fill = color
      scaleX = size / 16.0
      scaleY = size / 16.0
