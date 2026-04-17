package fdswarm.scoring

import fdswarm.model.Qso
import jakarta.inject.{Inject, Singleton}

@Singleton
class ArrlFdScorer @Inject() (
                               arrlScoringRules: ArrlScoringRules
                             ) extends ContestScorer:

  val name = "ARRL"

  def score(
             qsos: Seq[Qso],
             scoringConfig: ContestScoringConfig
           ): ScoreResult =
    val byMode =
      Map(
        "CW" -> qsos.count(q => scoringModeOf(q) == "CW"),
        "DI" -> qsos.count(q => scoringModeOf(q) == "DI"),
        "PH" -> qsos.count(q => scoringModeOf(q) == "PH")
      )

    val byBand =
      qsos.groupBy(_.bandMode.band).view.mapValues(_.size).toMap

    val rawPoints =
      qsos.map(qsoPoints).sum

    val multiplier =
      arrlPowerMultiplier(
        powerWatts = scoringConfig.powerWatts,
        powerSource = scoringConfig.powerSource
      )
    val breakdown =
      Map(
        "rawPoints" -> rawPoints.toDouble,
        "powerMultiplier" -> multiplier
      )
    ScoreResult(
      totalScore = (rawPoints * multiplier).toInt,
      rawPoints = rawPoints,
      multiplier = multiplier,
      totalQsos = qsos.size,
      byMode = byMode,
      byBand = byBand,
      breakdown = breakdown
    )

  private def arrlPowerMultiplier(
                                   powerWatts: Int,
                                   powerSource: PowerSource
                                 ): Double =
    val nonCommercial =
      powerSource match
        case PowerSource.Battery | PowerSource.Solar => true
        case _                                       => false

    arrlScoringRules.multiplierTiers
      .find(t => powerWatts <= t.maxPower && t.nonCommercial == nonCommercial)
      .orElse(arrlScoringRules.multiplierTiers.find(t => powerWatts <= t.maxPower))
      .map(_.multiplier)
      .getOrElse(1.0)

  private def qsoPoints(qso: Qso): Int =
    scoringModeOf(qso) match
      case "CW" => 2
      case "DI" => 2
      case _    => 1

  private def scoringModeOf(qso: Qso): String =
    qso.bandMode.mode.trim.toUpperCase match
      case "CW" => "CW"
      case "USB" | "LSB" | "SSB" | "FM" | "AM" | "PHONE" | "PH" => "PH"
      case _ => "DI"