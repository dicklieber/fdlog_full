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

package fdswarm.fx.tools

import fdswarm.util.OtelMetrics
import jakarta.inject.{Inject, Singleton}
import scalafx.Includes.*
import scalafx.geometry.Insets
import scalafx.scene.control.*
import scalafx.scene.layout.{Priority, VBox}
import scalafx.stage.Window

@Singleton
class MetricsDialog @Inject() (
    otelMetrics: OtelMetrics
):

  def show(ownerWindow: Window): Unit =
    val textArea = new TextArea:
      text = otelMetrics.scrape()
      editable = false
      prefWidth = 600
      prefHeight = 500
      vgrow = Priority.Always

    val refreshButton = new Button("Refresh"):
      onAction = _ => textArea.text = otelMetrics.scrape()

    val dialog = new Dialog[Unit]:
      title = "Metrics"
      headerText = "OpenTelemetry Metrics Dump"
      initOwner(ownerWindow)

    dialog.dialogPane().buttonTypes = Seq(ButtonType.Close)
    dialog.dialogPane().content = new VBox:
      spacing = 10
      padding = Insets(10)
      children = Seq(textArea, refreshButton)

    dialog.showAndWait()
