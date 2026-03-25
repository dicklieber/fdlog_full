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
import fdswarm.fx.GridColumns
import fdswarm.fx.contest.ContestConfigManager
import fdswarm.util.DurationFormat
import jakarta.inject.{Inject, Named, Singleton}
import scalafx.animation.{KeyFrame, Timeline}
import scalafx.beans.property.ObjectProperty
import scalafx.scene.Node
import scalafx.scene.control.Label
import scalafx.util.Duration
import java.time.{Duration as JDuration, ZonedDateTime}
import scalafx.Includes.*

@Singleton
class ContestTimerPanel @Inject()(
                                   contestManager: ContestConfigManager,
                                   @Named("fdswarm.contestTimerUpdateSec") contestTimerUpdateSec: Int
                                 ) extends LazyLogging:

  private val contestTimerLabel = new Label:
    styleClass += "contest-timer"
    minWidth = scalafx.scene.layout.Region.USE_PREF_SIZE

  enum TimeMode:
    case Before, During, After

  val timeModeProperty = ObjectProperty[TimeMode](this, "timeMode", TimeMode.Before)

  private var useMockTime: Boolean = false
  private var mockTime: ZonedDateTime = ZonedDateTime.now()

  def setMockTime(useFixed: Boolean, time: ZonedDateTime): Unit =
    useMockTime = useFixed
    mockTime = time
    updateContestTimer()

  private def updateContestTimer(): Unit =
    val now = if useMockTime then mockTime else ZonedDateTime.now()
    val config = contestManager.config
    val times = contestManager.contestTimesProperty.value

    val mode =
      if now.isBefore(times.start) then TimeMode.Before
      else if now.isAfter(times.end) then TimeMode.After
      else TimeMode.During

    timeModeProperty.value = mode

    val (msg, style) = mode match
      case TimeMode.Before =>
        (s"${config.contestType.name} ${times.start.getYear} starts in ${DurationFormat(JDuration.between(now, times.start))}", "contest-before")
      case TimeMode.After =>
        (s"${config.contestType.name} ${times.start.getYear} ended ${DurationFormat(JDuration.between(times.end, now))} ago.", "contest-after")
      case TimeMode.During =>
        (s"${config.contestType.name} ${times.start.getYear} ends in ${DurationFormat(JDuration.between(now, times.end))}", "contest-during")

    contestTimerLabel.text = msg
    contestTimerLabel.styleClass.removeAll("contest-before", "contest-during", "contest-after")
    contestTimerLabel.styleClass.add(style)

  private val timerTimeline = new Timeline:
    keyFrames = Seq(
      KeyFrame(Duration(contestTimerUpdateSec * 1000), onFinished = _ => updateContestTimer())
    )
    cycleCount = Timeline.Indefinite
  timerTimeline.play()
  updateContestTimer()

  def node: Node =
    val config = contestManager.config
    val times = contestManager.contestTimesProperty.value
    GridColumns.fieldSet(s"${config.contestType.name} ${times.start.getYear}", contestTimerLabel)
