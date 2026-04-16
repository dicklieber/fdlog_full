package fdswarm.scoring


import fdswarm.model.Qso

object NoopScorer extends ContestScorer:
  val name = "NONE"

  def score(qsos: Seq[Qso]): ScoreResult =
    ScoreResult(
      totalScore = 0,
      rawPoints = 0,
      multiplier = 1.0,
      totalQsos = qsos.size,
      byMode = Map.empty,
      byBand = Map.empty
    )