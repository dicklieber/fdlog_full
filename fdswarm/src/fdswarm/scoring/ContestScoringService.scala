package fdswarm.scoring

import fdswarm.fx.contest.{ContestConfigManager, ContestType}
import fdswarm.logging.LazyStructuredLogging
import fdswarm.logging.Locus.LogEntry
import fdswarm.store.QsoStore
import jakarta.inject.*
import nl.grons.metrics4.scala.DefaultInstrumented

import java.util.concurrent.atomic.{AtomicInteger, AtomicLong, AtomicReference}

@Singleton
class ContestScoringService @Inject() (
                                        qsoStore: QsoStore,
                                        contestConfigManager: ContestConfigManager,
                                        contestScoringConfigManager: ContestScoringConfigManager
                                      ) extends DefaultInstrumented
  with LazyStructuredLogging(LogEntry):

  private var scorer: ContestScorer =
    ContestScorerRegistry.forType(
      contestConfigManager.contestConfigProperty.value.contestType
    )

  private val finalScoreValue = new AtomicLong(0L)
  private val rawPointsValue = new AtomicLong(0L)
  private val totalQsosValue = new AtomicLong(0L)
  private val multiplierValue = new AtomicReference[Double](1.0)
  private val contestTypeValue = new AtomicInteger(
    toContestTypeMetricValue(
      contestConfigManager.contestConfigProperty.value.contestType
    )
  )

  metrics.gauge("fdswarm_contest_final_score")(finalScoreValue.get())
  metrics.gauge("fdswarm_contest_raw_qso_points")(rawPointsValue.get())
  metrics.gauge("fdswarm_contest_total_qsos")(totalQsosValue.get())
  metrics.gauge("fdswarm_contest_multiplier")(multiplierValue.get())
  metrics.gauge("fdswarm_contest_type")(contestTypeValue.get())

  contestConfigManager.contestConfigProperty.onChange { (_, oldConfig, newConfig) =>
    val oldType = oldConfig.contestType
    val newType = newConfig.contestType

    if oldType != newType then
      scorer = ContestScorerRegistry.forType(newType)
      contestTypeValue.set(toContestTypeMetricValue(newType))
      logger.info(
        "contestTypeChanged" -> s"$oldType->$newType",
        "scorer" -> scorer.name
      )
      recompute()
  }

  contestScoringConfigManager.contestScoringConfigProperty.onChange { (_, _, newConfig) =>
    logger.info(
      "contestScoringConfigChanged" -> true,
      "powerWatts" -> newConfig.powerWatts,
      "powerSource" -> newConfig.powerSource.toString,
      "enabledObjectives" -> newConfig.claimedObjectives.toSeq.sorted.mkString(","),
      "includeBonusesInLiveScore" -> newConfig.includeBonusesInLiveScore
    )
    recompute()
  }

  qsoStore.qsoCollection.onChange { (_, _) =>
    recompute()
  }

  recompute()

  private def recompute(): Unit =
    val qsos = qsoStore.all
    val scoringConfig = contestScoringConfigManager.current

    val result =
      scorer.score(
        qsos = qsos,
        scoringConfig = scoringConfig
      )

    finalScoreValue.set(result.totalScore.toLong)
    rawPointsValue.set(result.rawPoints.toLong)
    totalQsosValue.set(result.totalQsos.toLong)
    multiplierValue.set(result.multiplier)

    logger.info(
      "contestScoreRecomputed" -> true,
      "contestType" -> contestConfigManager.contestConfigProperty.value.contestType.toString,
      "scorer" -> scorer.name,
      "qsos" -> result.totalQsos,
      "rawPoints" -> result.rawPoints,
      "multiplier" -> result.multiplier,
      "finalScore" -> result.totalScore
    )

  private def toContestTypeMetricValue(contestType: ContestType): Int =
    contestType match
      case ContestType.NONE => 0
      case ContestType.WFD  => 1
      case ContestType.ARRL => 2

  private val lastResult =
    new java.util.concurrent.atomic.AtomicReference[ScoreResult](
      ScoreResult(0, 0, 1.0, 0, Map.empty, Map.empty)
    )

  def current: ScoreResult =
    lastResult.get()