package fdswarm.scoring

import fdswarm.model.Qso

object ArrlFdScorer extends ContestScorer:
  val name = "ARRL"

  def score(
      qsos: Seq[Qso],
      scoringConfig: ContestScoringConfig
    ): ScoreResult =
    val byMode =
      qsos.groupBy(_.bandMode.mode).view.mapValues(_.size).toMap

    val byBand =
      qsos.groupBy(_.bandMode.band).view.mapValues(_.size).toMap

    val rawPoints =
      qsos.map { q =>
        q.bandMode.mode.toUpperCase match
          case "CW" => 2
          case "DI" => 2
          case "PH" => 1
          case _    => 1
      }.sum

    val multiplier =
      val watts = scoringConfig.powerWatts

      if watts <= 5 then 5.0
      else if watts <= 150 then 2.0
      else 1.0

    ScoreResult(
      totalScore = (rawPoints * multiplier).toInt,
      rawPoints = rawPoints,
      multiplier = multiplier,
      totalQsos = qsos.size,
      byMode = byMode,
      byBand = byBand
    )
