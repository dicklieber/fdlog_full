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

package fdswarm.fx.qso

import com.typesafe.scalalogging.LazyLogging
import fdswarm.ContestDates
import fdswarm.fx.GridColumns
import fdswarm.fx.contest.{ContestConfig, ContestConfigManager, ContestType}
import fdswarm.util.DurationFormat
import jakarta.inject.{Inject, Named, Singleton}
import scalafx.animation.{KeyFrame, Timeline}
import scalafx.beans.property.{ObjectProperty, ReadOnlyObjectProperty}
import scalafx.scene.Node
import scalafx.scene.control.Label
import scalafx.util.Duration

import java.time.{ZonedDateTime, Duration as JDuration}

@Singleton
class ContestTimerPanel @Inject()(
                                   contestManager: ContestConfigManager,
                                   @Named("fdswarm.contestTimerUpdateSec") contestTimerUpdateSec: Int
                                 ) extends LazyLogging:
  private val contestConfigProperty: ReadOnlyObjectProperty[ContestConfig] = contestManager.contestConfigProperty
  private val timerTimeline = new Timeline:
    keyFrames = Seq(
      KeyFrame(Duration(contestTimerUpdateSec * 1000), onFinished = _ => updateContestTimeDisplay())
    )
    cycleCount = Timeline.Indefinite
  var contestType: ContestType = contestConfigProperty.value.contestType
  contestConfigProperty.onChange((_, _, newConfig) =>
    contestType = newConfig.contestType
    contestDates = contestType.dates()
    updateContestTimeDisplay())
  private val contestTimerLabel = new Label:
    styleClass += "contest-timer"
    minWidth = scalafx.scene.layout.Region.USE_PREF_SIZE

  enum TimeMode:
    case Before, During, After

  private var useMockTime: Boolean = false
  private var mockTime: ZonedDateTime = ZonedDateTime.now()
  var contestDates: ContestDates = contestType.dates()

  def setMockTime(useFixed: Boolean, time: ZonedDateTime): Unit =
    useMockTime = useFixed
    mockTime = time
    updateContestTimeDisplay()

  /**
   * show contest time stuff.
   *
   * @param contestDates
   */
  private def updateContestTimeDisplay(): Unit =
    val now = if useMockTime then mockTime else ZonedDateTime.now()
    val configProperty: ReadOnlyObjectProperty[ContestConfig] = contestConfigProperty

    val mode =
      if now.isBefore(contestDates.startUtc) then TimeMode.Before
      else if now.isAfter(contestDates.endUtc) then TimeMode.After
      else TimeMode.During

    val (msg, style) = mode match
      case TimeMode.Before =>
        (s"${contestType.name} ${contestDates.startUtc.getYear} starts in ${DurationFormat(JDuration.between(now, contestDates.endUtc))}", "contest-before")
      case TimeMode.After =>
        (s"${contestType.name} ${contestDates.startUtc.getYear} ended ${DurationFormat(JDuration.between(contestDates.endUtc, now))} ago.", "contest-after")
      case TimeMode.During =>
        (s"${contestType.name} ${contestDates.startUtc.getYear} ends in ${DurationFormat(JDuration.between(now, contestDates.endUtc))}", "contest-during")

    contestTimerLabel.text = msg
    contestTimerLabel.styleClass.removeAll("contest-before", "contest-during", "contest-after")
    contestTimerLabel.styleClass.add(style)
  timerTimeline.play()
  updateContestTimeDisplay()

  def node: Node =
    val config = contestConfigProperty
    GridColumns.fieldSet(s"${contestType.name} ${contestDates.startUtc.getYear}", contestTimerLabel)
