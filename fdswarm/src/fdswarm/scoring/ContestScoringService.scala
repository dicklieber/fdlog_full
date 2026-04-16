package fdswarm.scoring

import fdswarm.fx.contest.ContestConfigManager
import fdswarm.logging.Locus.Scoring
import fdswarm.logging.{LazyStructuredLogging, StructuredLogger}
import fdswarm.store.QsoStore
import jakarta.inject.*
import nl.grons.metrics4.scala.DefaultInstrumented

@Singleton
class ContestScoringService @Inject() (
                                        qsoStore: QsoStore,
                                        contestConfigManager: ContestConfigManager)
  extends  LazyStructuredLogging(Scoring) with DefaultInstrumented:

  // 🔥 current scorer (mutable on purpose)
  private var scorer: ContestScorer =
    ContestScorerRegistry.forType(
      contestConfigManager.contestConfigProperty.value.contestType
    )

  // 🔥 backing values (IMPORTANT: not plain gauge lambdas)
  private val scoreValue       = new java.util.concurrent.atomic.AtomicLong(0)
  private val rawPointsValue   = new java.util.concurrent.atomic.AtomicLong(0)
  private val multiplierValue  = new java.util.concurrent.atomic.AtomicReference[Double](1.0)
  private val totalQsosValue   = new java.util.concurrent.atomic.AtomicLong(0)

  // metrics
  metrics.gauge("fdswarm_contest_final_score")(scoreValue.get)
  metrics.gauge("fdswarm_contest_raw_qso_points")(rawPointsValue.get)
  metrics.gauge("fdswarm_contest_multiplier")(multiplierValue.get)
  metrics.gauge("fdswarm_contest_total_qsos")(totalQsosValue.get)

  // 🟢 1. react to contest type changes
  contestConfigManager.contestConfigProperty.onChange { (_, oldCfg, newCfg) =>
    val newType = newCfg.contestType
    val oldType = oldCfg.contestType

    if newType != oldType then
      scorer = ContestScorerRegistry.forType(newType)
      logger.info("contestTypeChanged" -> newType.toString)

      recompute()   // 🔥 immediate update
  }

  // 🟢 2. react to QSO changes
  qsoStore.qsoCollection.onChange { (_, _) =>
    recompute()
  }

  // 🟢 3. recompute (simple + correct first)
  private def recompute(): Unit =
    val qsos = qsoStore.all   // your thread-safe snapshot

    val result = scorer.score(qsos)

    scoreValue.set(result.totalScore)
    rawPointsValue.set(result.rawPoints)
    multiplierValue.set(result.multiplier)
    totalQsosValue.set(result.totalQsos)