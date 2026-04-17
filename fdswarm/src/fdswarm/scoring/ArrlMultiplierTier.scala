package fdswarm.scoring

import com.typesafe.config.{Config, ConfigObject}
import jakarta.inject.{Inject, Singleton}

case class ArrlMultiplierTier(
                               maxPower: Int,
                               nonCommercial: Boolean,
                               multiplier: Double
                             )

@Singleton
class ArrlScoringRules @Inject() (
                                   config: Config
                                 ):

  private val path = "fdswarm.scoring.arrl.multiplier-tiers"

  val multiplierTiers: Seq[ArrlMultiplierTier] =
    config
      .getObjectList(path)
      .toArray
      .toSeq
      .map(_.asInstanceOf[ConfigObject].toConfig)
      .map { cfg =>
        ArrlMultiplierTier(
          maxPower = cfg.getInt("max-power"),
          nonCommercial = cfg.getBoolean("non-commercial"),
          multiplier = cfg.getDouble("multiplier")
        )
      }
      .sortBy(_.maxPower)