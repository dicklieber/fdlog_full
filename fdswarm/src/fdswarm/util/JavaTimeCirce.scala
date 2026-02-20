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

import io.circe.*

import java.time.format.{DateTimeFormatter, DateTimeFormatterBuilder}
import java.time.*
import java.net.{URI, URL}

object JavaTimeCirce:

  given Encoder[URL] = Encoder.encodeString.contramap(_.toExternalForm)
  given Decoder[URL] = Decoder.decodeString.map(s => URI.create(s).toURL)

  given Encoder[LocalTime] = Encoder.encodeString.contramap(_.format(DateTimeFormatter.ISO_LOCAL_TIME))
  given Decoder[LocalTime] = Decoder.decodeString.map(LocalTime.parse(_, DateTimeFormatter.ISO_LOCAL_TIME))

  given Encoder[LocalDate] = Encoder.encodeString.contramap(_.format(DateTimeFormatter.ISO_LOCAL_DATE))
  given Decoder[LocalDate] = Decoder.decodeString.map(LocalDate.parse)

  given Encoder[LocalDateTime] = Encoder.encodeString.contramap(_.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
  given Decoder[LocalDateTime] = Decoder.decodeString.map(LocalDateTime.parse)

  given Encoder[ZonedDateTime] = Encoder.encodeString.contramap(_.format(DateTimeFormatter.ISO_ZONED_DATE_TIME))
  given Decoder[ZonedDateTime] = Decoder.decodeString.map(ZonedDateTime.parse)

  private def formatter = new DateTimeFormatterBuilder()
    .appendInstant(2) // Limit fractional digits to 2 on write.
    .toFormatter()

  given Encoder[Instant] = Encoder.encodeString.contramap(formatter.format)
  given Decoder[Instant] = Decoder.decodeString.map(Instant.parse)
