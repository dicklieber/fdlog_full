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
import fdswarm.fx.sections.Sections
import fdswarm.io.DirectoryProvider
import fdswarm.model.Callsign
import io.circe
import io.circe.parser.*
import io.circe.syntax.*
import jakarta.inject.{Inject, Singleton}
import scalafx.beans.property.{ObjectProperty, ReadOnlyObjectProperty}
import scalafx.scene.control.*
import scalafx.stage.Window

import java.time.*

@Singleton
final class ContestConfigManager @Inject()(
                                      productionDirectory: DirectoryProvider,
                                      qsoStore: fdswarm.store.QsoStore,
                                      filenameStamp: fdswarm.util.FilenameStamp,
                                      @Named("fdswarm.contestChangeIgnoreStatusSec") ignoreStatusSec: Int
                                    ) extends LazyLogging:

  private var lastRestartTime: Long = 0L

  def shouldIgnoreStatus: Boolean =
    val now = System.currentTimeMillis()
    (now - lastRestartTime) < (ignoreStatusSec * 1000L)
  
  private val contestFile: os.Path =
    productionDirectory() / "contest.json"
  /**
   * Intertnal state of [[ContestConfig]].
   */
  private var maybeContestConfig: Option[ContestConfig] = load()
  /**
   * This is the source of [[ContestConfig]].
   * Cannot be modified directly. Any changes must be done through [[setConfig]].
   */
  @throws[IllegalStateException]("If not initialized")
  def contestConfig: ContestConfig =
    maybeContestConfig.getOrElse(throw new IllegalStateException("ContestConfig not initialized"))

  /**
   * This is the only way to modify [[ContestConfig]].
   */
  def setConfig(newConfig: ContestConfig): Unit =
    maybeContestConfig = Option(newConfig)
    persist()

//  def classChars: String =
//    contestCatalog.getContest(contestConfig.contestType).map(_.classCharsString).getOrElse("")

  /**
   * 1. Rename qsosJournal.json to timestamped.qsosJournal.json
   * 2. Clear memory stores
   * 3. Save new config to contest.json and timestamped old contestconfig.json file
   *
   */
  def handleRestartContest(newConfig: ContestConfig): Unit =
    lastRestartTime = System.currentTimeMillis()
    archiveAndClear()
    setConfig(newConfig)
  

  private def load(): Option[ContestConfig] =
    try
      for
        file <- Option.when(os.exists(contestFile))(contestFile)
        contestConfig: ContestConfig <- decode(os.read(file)).toOption
      yield
        contestConfig
    catch
      case e:Throwable =>
        logger.error(s"Failed to load contest config from $contestFile", e)
        None

  def archiveAndClear(): Unit =
    qsoStore.archiveAndClear()
    persist()

  private def persist(): Unit =
    try
      val timestampedFile = productionDirectory() / s"${filenameStamp.build()}.contest.json"
      assert(maybeContestConfig.isDefined, "ContestConfig not initialized, but attempting to persist!")
      val json = maybeContestConfig.get.asJson.spaces2
      os.write.over(contestFile, json, createFolders = true)
      os.copy.over(contestFile, timestampedFile)
      logger.info(s"Persisted contest config to $contestFile and archived to $timestampedFile")
    catch
      case e: Throwable => logger.error(s"Failed to persist contest config to $contestFile", e)

