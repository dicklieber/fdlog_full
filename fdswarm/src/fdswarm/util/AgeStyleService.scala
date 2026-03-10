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

package fdswarm.util

import com.typesafe.config.Config
import java.time.Instant
import jakarta.inject.{Inject, Singleton}
import scala.jdk.CollectionConverters.*

@Singleton
class AgeStyleService @Inject()(config: Config):
  private val ageStyles: Map[String, AgeStyle] = 
    if config.hasPath("fdswarm.ageStyles") then
      config.getConfigList("fdswarm.ageStyles").asScala.map { styleConfig =>
        val name = styleConfig.getString("name")
        val olderStyle = styleConfig.getString("olderStyle")
        val thresholds = styleConfig.getConfigList("thresholds").asScala.map { t =>
          val d = t.getDuration("duration")
          val s = t.getString("style")
          (d, s)
        }.toSeq
        name -> new AgeStyle(thresholds*)(olderStyle)
      }.toMap
    else
      Map.empty

  def calc(ageStyleName: String, instant: Instant, now: Instant = Instant.now()): AgeStyle.StyleAndAge =
    ageStyles.get(ageStyleName) match
      case Some(style) => style.calc(instant, now)
      case None => 
        throw new IllegalArgumentException(s"AgeStyle '$ageStyleName' not found in configuration")
