package fdswarm.model.sections

import com.typesafe.config.Config
import jakarta.inject.{Inject, Singleton}

import scala.jdk.CollectionConverters.*

@Singleton
final class SectionsProvider @Inject()(config: Config):

  /** Loaded once at construction time. */
  val sectionsConfig: SectionsConfig =
    load()

  val sections: Seq[Section] =
    sectionsConfig.all

  private def load(): SectionsConfig =
    val root = "fdswarm.sections"

    def readSection(c: Config): Section =
      Section(
        code = c.getString("code"),
        name = c.getString("name")
      )

    def readSectionList(path: String): Seq[Section] =
      config.getConfigList(path).asScala.toSeq.map(readSection)

    val us = readSectionList(s"$root.US")
    val ca = readSectionList(s"$root.CA")
    val dx = readSection(config.getConfig(s"$root.DX"))

    SectionsConfig(US = us, CA = ca, DX = dx)