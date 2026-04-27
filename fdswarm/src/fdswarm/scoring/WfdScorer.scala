package fdswarm.scoring

import fdswarm.model.{Mode, Qso}
import jakarta.inject.{Inject, Singleton}

@Singleton
class WfdScorer @Inject() (
                            wfdScoringRules: WfdScoringRules
                          ) extends ContestScorer:

  val name = "WFD"

  def score(
             qsos: Seq[Qso],
             scoringConfig: ContestScoringConfig
           ): ScoreResult =
    val byMode =
      Map(
        "CW" -> qsos.count(q => scoringModeOf(q) == "CW"),
        "DIGI" -> qsos.count(q => scoringModeOf(q) == "DIGI"),
        "PH" -> qsos.count(q => scoringModeOf(q) == "PH")
      )

    val qsosByBand =
      qsos.groupBy(_.bandMode.band.name)

    val byBand =
      qsosByBand.view.mapValues(_.size).toMap

    val rawPoints =
      qsos.map(qsoPoints).sum

    val claimedObjectivePoints =
      scoringConfig.claimedObjectives.toSeq.distinct.map { id =>
        wfdScoringRules.flagObjectiveValues.getOrElse(id, 0)
      }.sum

    val qualifiedBandCount =
      qsosByBand.values.count(_.size >= 3)

    val bandObjectivePoints =
      if qualifiedBandCount >= 12 then 12
      else if qualifiedBandCount >= 6 then 6
      else 0

    val multiplier =
      1.0 + claimedObjectivePoints + bandObjectivePoints

    val breakdown =
      Map(
        "rawPoints" -> rawPoints.toDouble,
        "claimedObjectivePoints" -> claimedObjectivePoints.toDouble,
        "qualifiedBandCount" -> qualifiedBandCount.toDouble,
        "bandObjectivePoints" -> bandObjectivePoints.toDouble,
        "multiplier" -> multiplier
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

  private def qsoPoints(qso: Qso): Int =
    scoringModeOf(qso) match
      case "CW" => 2
      case "DIGI" => 2
      case _    => 1

  private def scoringModeOf(qso: Qso): String =
    qso.bandMode.mode match
      case Mode.CW => "CW"
      case Mode.PH => "PH"
      case Mode.DIGI => "DIGI"
