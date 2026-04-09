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

import java.time.{Duration, Instant}

/**
 * Utility class for styling objects based on age.
 *
 * @param thresholds sequence of (duration, styleClass) pairs, ordered by duration ascending.
 * @param olderStyle style class to return if the age is greater than all threshold durations.
 */
class AgeStyle(thresholds: (Duration, String)*)(olderStyle: String, purgeAfter: Option[Duration] = None):
  private val sortedThresholds = thresholds.sortBy(_._1)

  /**
   * Returns a style class based on the age (now - stamp).
   * @param stamp the timestamp to check.
   * @return the style class string.
   */
  def calc(stamp: Instant, now: Instant = Instant.now()): AgeStyle.StyleAndAge =
    val age = Duration.between(stamp, now)
    val needsPurge = purgeAfter.exists(
      age.compareTo(_) >= 0
    )
    sortedThresholds.find(t => age.compareTo(t._1) <= 0) match
      case Some((_, styleClass)) => AgeStyle.StyleAndAge(styleClass, age, needsPurge)
      case None                  => AgeStyle.StyleAndAge(olderStyle, age, needsPurge)

object AgeStyle:
  case class StyleAndAge(
    style: String,
    age: Duration,
    needsPurge: Boolean = false
  )
