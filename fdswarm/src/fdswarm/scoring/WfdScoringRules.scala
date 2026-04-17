package fdswarm.scoring

import com.typesafe.config.Config
import jakarta.inject.{Inject, Singleton}

@Singleton
class WfdScoringRules @Inject() (
                                  config: Config
                                ):

  private val path = "fdswarm.scoring.wfd.flag-objective-values"

  val flagObjectiveValues: Map[String, Int] =
    loadStringIntMap(config.getConfig(path))

  private def loadStringIntMap(cfg: Config): Map[String, Int] =
    cfg.entrySet().toArray.toSeq.map(_.asInstanceOf[java.util.Map.Entry[String, com.typesafe.config.ConfigValue]]).map { entry =>
      val key = entry.getKey
      key -> cfg.getInt(key)
    }.toMap