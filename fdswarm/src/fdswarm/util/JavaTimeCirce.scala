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

import java.net.{URI, URL}
import java.time.*
import java.time.format.DateTimeFormatter

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

  given Encoder[Instant] = Encoder.encodeString.contramap { instant =>
    val bb = java.nio.ByteBuffer.allocate(8)
    bb.putLong(instant.getEpochSecond)
    java.util.Base64.getUrlEncoder.withoutPadding().encodeToString(bb.array())
  }
  given Decoder[Instant] = Decoder.decodeString.emap { s =>
    scala.util.Try {
      val bytes = java.util.Base64.getUrlDecoder.decode(s)
      val bb = java.nio.ByteBuffer.wrap(bytes)
      Instant.ofEpochSecond(bb.getLong)
    }.recover {
      case _: IllegalArgumentException | _: java.nio.BufferUnderflowException =>
        Instant.parse(s)
    }.toEither.left.map(_.getMessage)
  }
