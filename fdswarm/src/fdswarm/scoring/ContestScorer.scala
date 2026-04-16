package fdswarm.scoring

import fdswarm.model.Qso

trait ContestScorer:
  def name: String

  def score(
             qsos: Seq[Qso],
             scoringConfig: ContestScoringConfig
           ): ScoreResult

case class ScoreResult(
    totalScore: Int,
    rawPoints: Int,
    multiplier: Double,
    totalQsos: Int,
    byMode: Map[String, Int],
    byBand: Map[String, Int])
