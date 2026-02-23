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
import fdswarm.fx.GridUtils
import fdswarm.fx.contest.ContestManager
import fdswarm.util.DurationFormat
import jakarta.inject.{Inject, Named, Singleton}
import scalafx.animation.{KeyFrame, Timeline}
import scalafx.scene.Node
import scalafx.scene.control.Label
import scalafx.util.Duration
import java.time.{Duration as JDuration, ZonedDateTime}
import scalafx.Includes.*

@Singleton
class ContestTimerPanel @Inject()(
                                   contestManager: ContestManager,
                                   @Named("fdswarm.contestTimerUpdateSec") contestTimerUpdateSec: Int
                                 ) extends LazyLogging:

  private val contestTimerLabel = new Label {
    styleClass += "contest-timer"
  }

  private def updateContestTimer(): Unit =
    val now = ZonedDateTime.now()
    val config = contestManager.config
    val (msg, style) =
      if now.isBefore(config.start) then
        (s"Contest starts in ${DurationFormat(JDuration.between(now, config.start))}", "contest-before")
      else if now.isAfter(config.end) then
        (s"Contest ended ${DurationFormat(JDuration.between(config.end, now))} ago.", "contest-after")
      else
        (s"Contest ends in ${DurationFormat(JDuration.between(now, config.end))}", "contest-during")

    contestTimerLabel.text = msg
    contestTimerLabel.styleClass.removeAll("contest-before", "contest-during", "contest-after")
    contestTimerLabel.styleClass.add(style)

  private val timerTimeline = new Timeline {
    keyFrames = Seq(
      KeyFrame(Duration(contestTimerUpdateSec * 1000), onFinished = _ => updateContestTimer())
    )
    cycleCount = Timeline.Indefinite
  }
  timerTimeline.play()
  updateContestTimer()

  def node: Node = GridUtils.fieldSet("Contest", contestTimerLabel)
