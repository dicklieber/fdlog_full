package fdswarm.fx.bands

import com.typesafe.config.{Config, ConfigRenderOptions}
import jakarta.inject.{Inject, Singleton}
import upickle.default.*

@Singleton
final class BandCatalog @Inject()(config: Config):
  private val key = "fdswarm.hamBands"

  private val renderOpts =
    ConfigRenderOptions.concise()
      .setJson(true)
      .setComments(false)
      .setOriginComments(false)

  /** Render the *list* at fdswarm.hamBands as strict JSON */
  /** Decode JSON → Seq[HamBand] */
  val hamBands: Seq[HamBand] =
    read[Seq[HamBand]](config.getValue(key).render(renderOpts))




