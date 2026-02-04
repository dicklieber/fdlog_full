package fdswarm

import fdswarm.fx.contest.Contest
import java.time.*
import upickle.default.*
import fdswarm.util.JavaTimePickle.given

final case class ContestDates(startUtc: ZonedDateTime, endUtc: ZonedDateTime) derives ReadWriter

object ContestDateCalculator:

  // ---- Public API -----------------------------------------------------------

  /** One entry point: compute UTC window for a contest + year. */
  def datesFor(contest: Contest, year: Int): ContestDates =
    contest match
      case Contest.WFD =>
        val (sat, sun) = lastFullWeekend(year, Month.JANUARY)
        ContestDates(
          startUtc = sat.atTime(19, 0).atZone(ZoneOffset.UTC),  // 1900Z Sat
          endUtc   = sun.atTime(18, 59).atZone(ZoneOffset.UTC)  // 1859Z Sun
        )

      case Contest.ARRL =>
        val (sat, sun) = nthFullWeekend(year, Month.JUNE, n = 4) // 4th full weekend in June
        ContestDates(
          startUtc = sat.atTime(18, 0).atZone(ZoneOffset.UTC),  // 1800Z Sat
          endUtc   = sun.atTime(20, 59).atZone(ZoneOffset.UTC)  // 2059Z Sun
        )

  // ---- Helpers --------------------------------------------------------------

  /** Nth full weekend (Sat/Sun both in the same month), n is 1-based. */
  private def nthFullWeekend(year: Int, month: Month, n: Int): (LocalDate, LocalDate) =
    require(n >= 1, s"n must be >= 1, got $n")

    val (firstSat, firstSun) = firstFullWeekend(year, month)
    (firstSat.plusWeeks((n - 1).toLong), firstSun.plusWeeks((n - 1).toLong))

  /** First full weekend (Sat/Sun both in the same month). */
  private def firstFullWeekend(year: Int, month: Month): (LocalDate, LocalDate) =
    val firstDay = LocalDate.of(year, month, 1)

    val daysToSat =
      (DayOfWeek.SATURDAY.getValue - firstDay.getDayOfWeek.getValue + 7) % 7

    val sat = firstDay.plusDays(daysToSat.toLong)
    val sun = sat.plusDays(1)

    (sat, sun)

  /** Last full weekend (Sat/Sun both in the same month). */
  private def lastFullWeekend(year: Int, month: Month): (LocalDate, LocalDate) =
    val lastDay = LocalDate.of(year, month, month.length(Year.isLeap(year.toLong)))

    val daysBackToSat =
      (lastDay.getDayOfWeek.getValue - DayOfWeek.SATURDAY.getValue + 7) % 7

    var sat = lastDay.minusDays(daysBackToSat.toLong)
    var sun = sat.plusDays(1)

    // If Sunday spills into the next month, go back a week
    if sun.getMonth != month then
      sat = sat.minusWeeks(1)
      sun = sat.plusDays(1)

    (sat, sun)
