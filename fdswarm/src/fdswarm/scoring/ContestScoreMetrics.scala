package fdswarm.scoring

import fdswarm.model.Qso
import jakarta.inject.Singleton
import nl.grons.metrics4.scala.DefaultInstrumented

import java.util.concurrent.atomic.AtomicInteger

enum ContestKind derives CanEqual:
  case ArrlFieldDay2026
  case WinterFieldDay2026

enum ScoringMode derives CanEqual:
  case Phone, CW, Digital

enum ArrlPowerSource derives CanEqual:
  case NonCommercial
  case CommercialOrGenerator

final case class ArrlFieldDayConfig(
                                     highestPowerWatts: BigDecimal,
                                     powerSource: ArrlPowerSource
                                   ):
  def powerMultiplier: Int =
    if highestPowerWatts <= BigDecimal(5) then
      powerSource match
        case ArrlPowerSource.NonCommercial         => 5
        case ArrlPowerSource.CommercialOrGenerator => 2
    else if highestPowerWatts <= BigDecimal(100) then 2
    else 1

final case class ContestScoreConfig(
                                     contestKind: ContestKind,
                                     arrl: Option[ArrlFieldDayConfig] = None
                                   )

@Singleton
final class ContestScoreMetrics extends DefaultInstrumented:

  private val totalQsos = new AtomicInteger(0)
  private val rawPoints = new AtomicInteger(0)
  private val multiplier = new AtomicInteger(1)
  private val finalScore = new AtomicInteger(0)

  metrics.gauge("fdswarm_contest_total_qsos")(totalQsos.get())
  metrics.gauge("fdswarm_contest_raw_qso_points")(rawPoints.get())
  metrics.gauge("fdswarm_contest_multiplier")(multiplier.get())
  metrics.gauge("fdswarm_contest_final_score")(finalScore.get())

  def refresh(qsos: Iterable[Qso], config: ContestScoreConfig): Unit =
    val unique = qsos.toVector
    val points = unique.map(qsoPoints).sum
    val mult = config.arrl.map(_.powerMultiplier).getOrElse(1)
    totalQsos.set(unique.size)
    rawPoints.set(points)
    multiplier.set(mult)
    finalScore.set(points * mult)

  private def qsoPoints(q: Qso): Int =
    val m = q.bandMode.mode.toUpperCase
    m match
      case "CW" => 2
      case "USB" | "LSB" | "SSB" | "FM" | "AM" => 1
      case _ => 2
