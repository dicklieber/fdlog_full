package fdlog.util

import upickle.default.*

import java.time.format.{DateTimeFormatter, DateTimeFormatterBuilder}
import java.time.*

object JavaTimePickle:

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