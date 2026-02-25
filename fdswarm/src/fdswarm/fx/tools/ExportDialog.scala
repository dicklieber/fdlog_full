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

import com.typesafe.scalalogging.LazyLogging
import fdswarm.fx.contest.ContestManager
import fdswarm.fx.station.StationStore
import fdswarm.io.DirectoryProvider
import fdswarm.store.QsoStore
import fdswarm.util.FilenameStamp
import jakarta.inject.{Inject, Singleton}
import scalafx.Includes.*
import scalafx.geometry.Insets
import scalafx.scene.control.*
import scalafx.scene.layout.{GridPane, HBox, Priority}
import scalafx.stage.{DirectoryChooser, Window}

@Singleton
final class ExportDialog @Inject()(
                                    qsoStore: QsoStore,
                                    directoryProvider: DirectoryProvider,
                                    filenameStamp: FilenameStamp,
                                    contestManager: ContestManager,
                                    stationStore: StationStore
                                  ) extends LazyLogging:

  private enum ExportFormat(val extension: String, val description: String):
    case ADIF extends ExportFormat("adi", "ADIF Files (*.adi)")
    case CABRILLO extends ExportFormat("cbr", "Cabrillo Files (*.cbr)")
    case JSON extends ExportFormat("json", "JSON Files (*.json)")
    case ZIP  extends ExportFormat("zip", "Zip Files (*.zip)")

    override def toString: String = description

  def show(ownerWindow: Window): Unit =
    val initialFilename = filenameStamp.build()
    
    val filenameField = new TextField:
      text = initialFilename
      promptText = "Filename (without extension)"
      prefWidth = 350

    val formatCombo = new ComboBox[ExportFormat](ExportFormat.values.toIndexedSeq):
      value = ExportFormat.ADIF

    val directoryField = new TextField:
      text = System.getProperty("user.home")
      promptText = "Export Directory"
      editable = false
      hgrow = Priority.Always

    val browseButton = new Button("Browse..."):
      onAction = _ =>
        val chooser = new DirectoryChooser:
          title = "Select Export Directory"
          initialDirectory = new java.io.File(directoryField.text.value)
        val selectedDir = chooser.showDialog(ownerWindow)
        if selectedDir != null then
          directoryField.text = selectedDir.getAbsolutePath

    val dialog = new Dialog[ButtonType]:
      initOwner(ownerWindow)
      title = "Export"
      headerText = "Export QSO Log"
      resizable = true

    val exportButtonType = new ButtonType("Export", ButtonBar.ButtonData.OKDone)
    dialog.dialogPane().buttonTypes = Seq(ButtonType.Cancel, exportButtonType)
    
    // Set a good width for the dialog
    dialog.dialogPane().setPrefWidth(550)

    val content = new GridPane:
      hgap = 10
      vgap = 10
      padding = Insets(20)

      add(new Label("Filename:"), 0, 0)
      add(filenameField, 1, 0)

      add(new Label("Format:"), 0, 1)
      add(formatCombo, 1, 1)

      add(new Label("To Folder:"), 0, 2)
      val folderBox = new HBox(5, directoryField, browseButton)
      add(folderBox, 1, 2)

    dialog.dialogPane().content = content
    dialog.resultConverter = (btn: ButtonType) => btn

    val result = dialog.showAndWait()
    if result.contains(exportButtonType) then
      val baseName = filenameField.text.value.trim
      val format = formatCombo.value.value
      val dir = os.Path(directoryField.text.value)
      val fullPath = dir / s"$baseName.${format.extension}"
      
      try
        executeExport(fullPath, format)
        val alert = new Alert(Alert.AlertType.Information):
          initOwner(ownerWindow)
          title = "Export Success"
          headerText = "Export completed successfully"
          contentText = s"File saved to: $fullPath"
        alert.showAndWait()
      catch
        case e: Exception =>
          logger.error(s"Export failed to $fullPath", e)
          val alert = new Alert(Alert.AlertType.Error):
            initOwner(ownerWindow)
            title = "Export Failed"
            headerText = "An error occurred during export"
            contentText = e.getMessage
          alert.showAndWait()

  private def executeExport(path: os.Path, format: ExportFormat): Unit =
    format match
      case ExportFormat.ADIF =>
        val qsos = qsoStore.all
        val adif = fdswarm.io.AdifExporter.exportQsos(qsos)
        os.write.over(path, adif)
      case ExportFormat.CABRILLO =>
        val qsos = qsoStore.all
        val station = stationStore.station.value
        val contest = contestManager.config.contest
        val cabrillo = fdswarm.io.CabrilloExporter.exportQsos(qsos, station, contest)
        os.write.over(path, cabrillo)
      case ExportFormat.JSON =>
        val qsos = qsoStore.all
        val json = fdswarm.io.JsonExporter.exportQsos(qsos)
        os.write.over(path, json)
      case ExportFormat.ZIP =>
        fdswarm.io.ZipExporter.zipDirectory(directoryProvider(), path)
