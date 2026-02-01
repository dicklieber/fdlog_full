package fdswarm.fx.bands

import com.typesafe.config.{Config, ConfigRenderOptions}
import fdswarm.model.BandMode.Mode
import jakarta.inject.{Inject, Singleton}
import upickle.default.*

import java.util
import scala.jdk.CollectionConverters.*

/**
 * This is loaded from application.conf.
 */
@Singleton
final class ModeCatalog @Inject()(config: Config):
  val modes: Seq[Mode] = config.getStringList("fdswarm.modes").asScala.toSeq
  
