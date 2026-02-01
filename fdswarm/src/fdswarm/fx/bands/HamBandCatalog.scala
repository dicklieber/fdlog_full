package fdswarm.fx.bands

import com.typesafe.config.{Config, ConfigRenderOptions}
import jakarta.inject.{Inject, Singleton}
import upickle.default.*

@Singleton
final class HamBandCatalog @Inject()(config: Config):

  // IMPORTANT: define this before any vals that use it
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

  private val byName: Map[String, HamBand] =
    hamBands
      .flatMap(b => Seq(b.bandName -> b, b.bandName.toLowerCase -> b))
      .toMap

  def all: Seq[HamBand] = hamBands

  def get(band: String): Option[HamBand] =
    byName.get(band)