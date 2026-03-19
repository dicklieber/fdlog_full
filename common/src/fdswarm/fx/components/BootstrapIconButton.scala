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

package fdswarm.fx.components

import scalafx.beans.property.StringProperty
import scalafx.scene.control.{Button, Tooltip}
import scalafx.scene.image.{Image, ImageView}
import scalafx.scene.layout.{Priority, StackPane}
import scalafx.Includes.*

/**
 * A reusable ScalaFX Button that displays an icon (e.g., Bootstrap icons) 
 * filling most of the button's area. 
 * Shows just the icon with no standard button styling.
 */
class BootstrapIconButton(iconPathInitial: String) extends Button:

  private val _iconPath = new StringProperty(this, "iconPath", iconPathInitial)

  def iconPath: StringProperty = _iconPath
  def iconPath_=(v: String): Unit = _iconPath.value = v

  // For SVG support in JavaFX, we'll use a Region with -fx-shape
  // or a simple ImageView if it's a PNG/JPG. 
  // Since Bootstrap Icons are SVGs, we'll try to extract the path.
  private val iconNode = new StackPane():
    style <== iconPath.delegate.map { p =>
      if p == null || p.isEmpty then ""
      else
        val resource = getClass.getResource("/" + p)
        if (resource == null) {
          System.err.println(s"[ERROR] BootstrapIconButton: Resource not found: /$p")
          ""
        } else if (p.toLowerCase.endsWith(".svg")) {
          // Extract path data from SVG. 
          // For simple Bootstrap Icons, it's usually <path d="..."/>
          try {
            val content = os.read(os.Path(java.nio.file.Paths.get(resource.toURI)))
            val pathRegex = "d=\"([^\"]+)\"".r
            val paths = pathRegex.findAllMatchIn(content).map(_.group(1)).toList
            if (paths.nonEmpty) {
              // Combine multiple paths if present
              val combinedPath = paths.mkString(" ")
              s"-fx-shape: \"$combinedPath\"; -fx-background-color: currentColor; -fx-min-width: 16; -fx-min-height: 16;"
            } else ""
          } catch {
            case e: Exception =>
              System.err.println(s"[ERROR] BootstrapIconButton: Failed to parse SVG: $p - ${e.getMessage}")
              ""
          }
        } else ""
    }
    // Bind size to button size minus padding
    prefWidth <== width * 0.7
    prefHeight <== height * 0.7
    maxWidth <== width * 0.7
    maxHeight <== height * 0.7

  // If it's not an SVG, use ImageView as fallback
  private val iconView = new ImageView():
    image <== iconPath.delegate.map { p =>
      if p == null || p.isEmpty || p.toLowerCase.endsWith(".svg") then null
      else
        val resource = getClass.getResource("/" + p)
        if (resource == null) null
        else new Image(resource.toExternalForm).delegate
    }
    preserveRatio = true
    fitWidth <== width * 0.8
    fitHeight <== height * 0.8

  graphic = new StackPane {
    children = Seq(iconNode, iconView)
  }

  // Remove standard button styling
  style = "-fx-background-color: transparent; -fx-padding: 2; -fx-cursor: hand; -fx-text-fill: black;"

  // Hover effect
  onMouseEntered = _ =>
    style = "-fx-background-color: lightgray; -fx-padding: 2; -fx-cursor: hand; -fx-background-radius: 5; -fx-text-fill: black;"

  onMouseExited = _ =>
    style = "-fx-background-color: transparent; -fx-padding: 2; -fx-cursor: hand; -fx-text-fill: black;"

  // Ensure it doesn't grow unnecessarily but stays square-ish
  minWidth = 24
  minHeight = 24
  prefWidth = 32
  prefHeight = 32
  maxWidth = 48
  maxHeight = 48

object BootstrapIconButton:
  def apply(iconPath: String): BootstrapIconButton =
    new BootstrapIconButton(iconPath)