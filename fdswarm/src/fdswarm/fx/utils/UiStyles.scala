package fdswarm.fx.utils

import scalafx.scene.Scene
import scalafx.scene.control.DialogPane

/**
 * Applies the app.css stylesheet to a scene, dialogPane, etc.
 * ScalaFX stylesheets don't automatically get applied, this makes then work fairly automatically.
 */
object UiStyles:

  private val AppCssResource = "/styles/app.css"

  private lazy val appCss: String =
    val url = Option(getClass.getResource(AppCssResource)).getOrElse {
      throw new IllegalStateException(s"Missing stylesheet resource: $AppCssResource")
    }
    url.toExternalForm

  def applyTo(scene: Scene): Unit =
    if !scene.stylesheets.contains(appCss) then
      scene.stylesheets.add(appCss)

  def applyTo(scene: javafx.scene.Scene): Unit =
    if !scene.getStylesheets.contains(appCss) then
      scene.getStylesheets.add(appCss)

  def applyTo(dialogPane: DialogPane): Unit =
    if !dialogPane.stylesheets.contains(appCss) then
      dialogPane.stylesheets.add(appCss)

  def applyTo(dialogPane: javafx.scene.control.DialogPane): Unit =
    if !dialogPane.getStylesheets.contains(appCss) then
      dialogPane.getStylesheets.add(appCss)