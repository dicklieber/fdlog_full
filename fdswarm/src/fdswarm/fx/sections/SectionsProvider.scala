/*
 * Copyright (c) 2026. Dick Lieber, WA9NNN
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package fdswarm.fx.sections

import com.typesafe.config.Config
import jakarta.inject.{Inject, Singleton}

import scala.jdk.CollectionConverters.*

@Singleton
class SectionsProvider @Inject()(config: Config):

  val sectionGroups: Seq[SectionGroup] =
    config.getConfigList("fdswarm.sections").asScala.map { groupConfig =>
      val name = groupConfig.getString("name")
      val sections = groupConfig.getConfigList("sections").asScala.map { sectionConfig =>
        Section(
          sectionConfig.getString("code"),
          sectionConfig.getString("name")
        )
      }.toSeq
      SectionGroup(name, sections)
    }.toSeq

  val allSections: Seq[Section] = sectionGroups.flatMap(_.sections)
