package fdswarm.scoring

import fdswarm.model.Qso

object ArrlFdScorer extends ContestScorer:
  val name = "ARRL"

  def score(
             qsos: Seq[Qso],
             scoringConfig: ContestScoringConfig
           ): ScoreResult =
    val byMode =
      qsos.groupBy(q => scoringModeOf(q)).view.mapValues(_.size).toMap

    val byBand =
      qsos.groupBy(_.bandMode.band).view.mapValues(_.size).toMap

    val rawPoints =
      qsos.map(qsoPoints).sum

    val multiplier =
      arrlPowerMultiplier(
        powerWatts = scoringConfig.powerWatts,
        powerSource = scoringConfig.powerSource
      )

    ScoreResult(
      totalScore = (rawPoints * multiplier).toInt,
      rawPoints = rawPoints,
      multiplier = multiplier,
      totalQsos = qsos.size,
      byMode = byMode,
      byBand = byBand
    )

  private def arrlPowerMultiplier(
                                   powerWatts: Int,
                                   powerSource: PowerSource
                                 ): Double =
    val nonCommercial =
      powerSource match
        case PowerSource.Battery | PowerSource.Solar => true
        case _                                       => false

    if powerWatts <= 5 then
      if nonCommercial then 5.0 else 2.0
    else if powerWatts <= 100 then
      2.0
    else
      1.0

  private def qsoPoints(qso: Qso): Int =
    scoringModeOf(qso) match
      case "CW" => 2
      case "DI" => 2
      case _    => 1

  private def scoringModeOf(qso: Qso): String =
    qso.bandMode.mode.trim.toUpperCase match
      case "CW"                                => "CW"
      case "USB" | "LSB" | "SSB" | "FM" | "AM" | "PHONE" | "PH" => "PH"
      case _                                   => "DI"