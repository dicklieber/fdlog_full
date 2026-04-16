package fdswarm.scoring

import fdswarm.model.Qso

object WfdScorer extends ContestScorer:

  val name = "WFD"

  def score(qsos: Seq[Qso]): ScoreResult =
    val byMode = qsos.groupBy(_.bandMode.mode).view.mapValues(_.size).toMap
    val byBand = qsos.groupBy(_.bandMode.band).view.mapValues(_.size).toMap

    val rawPoints =
      qsos.map { q =>
        q.bandMode.mode match
          case "CW" => 2
          case "DI" => 2
          case _    => 1
      }.sum

    val multiplier = 1.0 // placeholder

    ScoreResult(
      totalScore = (rawPoints * multiplier).toInt,
      rawPoints = rawPoints,
      multiplier = multiplier,
      totalQsos = qsos.size,
      byMode = byMode,
      byBand = byBand
    )