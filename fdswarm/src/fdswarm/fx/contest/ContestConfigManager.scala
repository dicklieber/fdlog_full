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

package fdswarm.fx.contest

import com.google.inject.name.Named
import com.typesafe.scalalogging.LazyLogging
import fdswarm.fx.discovery.DiscoveryWire
import fdswarm.fx.sections.Sections
import fdswarm.io.DirectoryProvider
import fdswarm.model.Callsign
import fdswarm.util.NodeIdentity
import io.circe.parser.*
import io.circe.syntax.*
import jakarta.inject.{Inject, Singleton}
import scalafx.beans.property.ObjectProperty
import scalafx.scene.control.*
import scalafx.stage.Window

import java.time.*

@Singleton
final class ContestConfigManager @Inject()(
                                      productionDirectory: DirectoryProvider,
                                      contestCatalog: ContestCatalog,
                                      sections: Sections,
                                      qsoStore: fdswarm.store.QsoStore,
                                      filenameStamp: fdswarm.util.FilenameStamp,
                                      @Named("fdswarm.contestChangeIgnoreStatusSec") ignoreStatusSec: Int
                                    ) extends LazyLogging:

  private var lastRestartTime: Long = 0L

  def shouldIgnoreStatus: Boolean =
    val now = System.currentTimeMillis()
    (now - lastRestartTime) < (ignoreStatusSec * 1000L)

  private val file: os.Path =
    productionDirectory() / "contest.json"
  /**
   * Exposes the persisted state as an observable property so UIs can react immediately.
   * This is the sole souce of [[ContestConfig]].
   */
  val configProperty: ObjectProperty[ContestConfig] = ObjectProperty(load())

  val contestTimesProperty: ObjectProperty[ContestTimes] =
    val now = ZonedDateTime.now()
    val initialDates = configProperty.value.contestType.dates(now.getYear)
    ObjectProperty(ContestTimes(initialDates.startUtc, initialDates.endUtc))

  // Keep persist in sync with config
  configProperty.onChange { (prop, oldConfig, newConfig) =>
    if (oldConfig == null || oldConfig.contestType != newConfig.contestType) {
      val now = ZonedDateTime.now()
      val newDates = newConfig.contestType.dates(now.getYear)
      contestTimesProperty.value = ContestTimes(newDates.startUtc, newDates.endUtc)
    }
    persist()
  }

  def contestConfig: ContestConfig = configProperty.value

  def configExists: Boolean = os.exists(file)

  def setConfig(newConfig: ContestConfig): Unit =
    configProperty.value = newConfig

  def classChars: String =
    contestCatalog.getContest(contestConfig.contestType).map(_.classCharsString).getOrElse("")
  

  def handleRestartContest(newConfig: ContestConfig): Unit =
    // 1. Rename qsosJournal.json
    // 2. Clear memory stores
    // 3. Save new config to contest.json and timestamped file
    // 4. Update the local property
    lastRestartTime = System.currentTimeMillis()
    archiveAndClear()
    setConfig(newConfig)

  private def promptForYear(ownerWindow: Window): Option[Int] =
    val d = new TextInputDialog(LocalDate.now().getYear.toString):
      initOwner(ownerWindow)
      title = "Select Year"
      headerText = "Enter the year for the contest"
      contentText = "Year:"

    d.showAndWait().flatMap(_.toIntOption)

//  private def updateZonedDateTimeControl(form: MyCaseForm[?], fieldName: String, zdt: ZonedDateTime): Unit =
//    val field = form.fieldHandlers.find(_.name == fieldName).get
//    field.control() match {
//      case Some(editor: ZonedDateTimeEditor) => editor.value = zdt
//      case _ =>
//        // Fallback for any other control types if necessary, though currently it should be ZonedDateTimeEditor
//        logger.warn(s"Control for $fieldName is not a ZonedDateTimeEditor")
//    }

  // ---- persistence ----------------------------------------------------------

  private def persist(): Unit =
    try
      val timestampedFile = productionDirectory() / s"${filenameStamp.build()}.contest.json"
      val json = configProperty.value.asJson.spaces2
      os.write.over(file, json, createFolders = true)
      os.copy.over(file, timestampedFile)
      logger.info(s"Persisted contest config to $file and archived to $timestampedFile")
    catch
      case e: Throwable => logger.error(s"Failed to persist contest config to $file", e)

  def archiveAndClear(): Unit =
    qsoStore.archiveAndClear()
    persist()

  def load(): ContestConfig =
    try
      if os.exists(file) then
        val json = os.read(file)
        decode[ContestConfig](json).toTry.get
      else
        defaultConfig()
    catch
      case e: Throwable =>
        logger.warn(s"Failed to load contest config from $file: ${e.getMessage}. Using default.")
        defaultConfig()

  def defaultConfig(): ContestConfig =
    val sectionCode = sections.all.headOption.map(_.code).getOrElse("CT")
    ContestConfig(ContestType.WFD, Callsign("W1AW"), 1, "O", sectionCode)
