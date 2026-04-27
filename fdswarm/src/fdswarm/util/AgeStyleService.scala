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

import com.typesafe.config.{Config, ConfigValueType}
import jakarta.inject.{Inject, Singleton}

import java.time.Instant
import scala.jdk.CollectionConverters.*

@Singleton
class AgeStyleService @Inject()(config: Config):
  private val ageStyles: Map[String, AgeStyle] =
    if config.hasPath("fdswarm.ageStyles") then
      loadAgeStyles(
        config.getConfig("fdswarm.ageStyles")
      )
    else
      Map.empty

  private def loadAgeStyles(
    ageStylesConfig: Config
  ): Map[String, AgeStyle] =
    config.getValue("fdswarm.ageStyles").valueType() match
      case ConfigValueType.LIST =>
        config.getConfigList("fdswarm.ageStyles").asScala.map { styleConfig =>
          val name = styleConfig.getString("name")
          name -> buildAgeStyle(styleConfig)
        }.toMap
      case ConfigValueType.OBJECT =>
        ageStylesConfig.root().keySet().asScala.map { styleName =>
          val styleConfig = ageStylesConfig.getConfig(styleName)
          styleName -> buildAgeStyle(styleConfig)
        }.toMap
      case other =>
        throw new IllegalArgumentException(
          s"Unsupported fdswarm.ageStyles type: $other"
        )

  private def buildAgeStyle(
    styleConfig: Config
  ): AgeStyle =
    val thresholds = styleConfig.getConfigList("thresholds").asScala.map { thresholdConfig =>
      val duration = thresholdConfig.getDuration(
        "duration"
      )
      val styleName = thresholdConfig.getString(
        "style"
      )
      (duration, styleName)
    }.toSeq
    val olderStyle =
      if styleConfig.hasPath("olderStyle") then styleConfig.getString("olderStyle")
      else thresholds.lastOption.map(_._2).getOrElse {
        throw new IllegalArgumentException(
          "Age style thresholds cannot be empty when olderStyle is not provided"
        )
      }
    val purgeAfter =
      if styleConfig.hasPath("purgeAfter") then
        Some(
          styleConfig.getDuration(
            "purgeAfter"
          )
        )
      else
        None
    new AgeStyle(
      thresholds*
    )(
      olderStyle = olderStyle,
      purgeAfter = purgeAfter
    )

  def calc(
    ageStyleName: String,
    instant: Instant,
    now: Instant = Instant.now()
  ): AgeStyle.StyleAndAge =
    ageStyles.get(ageStyleName) match
      case Some(style) => style.calc(instant, now)
      case None => 
        throw new IllegalArgumentException(s"AgeStyle '$ageStyleName' not found in configuration")
