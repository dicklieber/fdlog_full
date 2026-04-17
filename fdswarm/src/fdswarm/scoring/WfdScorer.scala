package fdswarm.scoring

import fdswarm.model.Qso

object WfdScorer extends ContestScorer:
  val name = "WFD"

  private val FlagObjectiveValues: Map[String, Int] =
    Map(
      "away-from-home" -> 3,
      "qrp" -> 4,
      "alternative-power" -> 1,
      "multiple-antennas" -> 1,
      "satellite-qso" -> 1,
      "winlink-message" -> 1,
      "copy-bulletin" -> 1
    )

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

    val qsosByBand =
      qsos.groupBy(_.bandMode.band)

    val byBand =
      qsosByBand.view.mapValues(_.size).toMap

    val rawPoints =
      qsos.map(qsoPoints).sum

    val claimedObjectivePoints =
      scoringConfig.claimedObjectives.toSeq.distinct.map { id =>
        FlagObjectiveValues.getOrElse(id, 0)
      }.sum

    val qualifiedBandCount =
      qsosByBand.values.count(_.size >= 3)

    val bandObjectivePoints =
      if qualifiedBandCount >= 12 then 12
      else if qualifiedBandCount >= 6 then 6
      else 0

    val multiplier =
      1.0 + claimedObjectivePoints + bandObjectivePoints

    ScoreResult(
      totalScore = (rawPoints * multiplier).toInt,
      rawPoints = rawPoints,
      multiplier = multiplier,
      totalQsos = qsos.size,
      byMode = byMode,
      byBand = byBand
    )

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