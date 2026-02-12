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

import upickle.default.*

import java.time.format.{DateTimeFormatter, DateTimeFormatterBuilder}
import java.time.*
import java.net.{URI, URL}

object JavaTimePickle:

  given ReadWriter[URL] = readwriter[String].bimap[URL](
    _.toExternalForm,
    s => URI.create(s).toURL
  )

  given localTimeRW: ReadWriter[LocalTime] = readwriter[String].bimap[LocalTime](
    time => time.format(DateTimeFormatter.ISO_LOCAL_TIME), // Serialize as String
    str => LocalTime.parse(str, DateTimeFormatter.ISO_LOCAL_TIME) // Deserialize back
  )

  given ReadWriter[LocalDate] = readwriter[String]
    .bimap[LocalDate](
      _.format(DateTimeFormatter.ISO_LOCAL_DATE),
      LocalDate.parse
    )

  given ReadWriter[LocalDateTime] = readwriter[String]
    .bimap[LocalDateTime](
      _.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
      LocalDateTime.parse
    )

  given ReadWriter[ZonedDateTime] = readwriter[String]
    .bimap[ZonedDateTime](
      _.format(DateTimeFormatter.ISO_ZONED_DATE_TIME),
      ZonedDateTime.parse
    )

  private def formatter = new DateTimeFormatterBuilder()
    .appendInstant(2) // Limit fractional digits to 2 on write.
    .toFormatter()

  given ReadWriter[Instant] = readwriter[String].bimap[Instant](
    (x: Instant) => {
      val d = formatter.format(x)
      d
    },
    (str: String) => {
      val accessor: Instant = Instant.parse(str) // Handles 2 decimal place seconds just fine.
      accessor
    }
  )