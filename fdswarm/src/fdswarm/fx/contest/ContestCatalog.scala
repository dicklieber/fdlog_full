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

package fdswarm.fx.contest

import com.typesafe.config.{Config, ConfigRenderOptions}
import jakarta.inject.{Inject, Singleton}
import upickle.default.*

case class ClassCharConfig(
                            ch: String,
                            description: String
                          ) derives ReadWriter

case class Contest(
                    contestType: ContestType,
                    classChars: Seq[ClassCharConfig]
                  ) derives ReadWriter

@Singleton
final class ContestCatalog @Inject()(config: Config):
  /** Render the *list* at fdswarm.contests as strict JSON and decode */
  private val key = "fdswarm.contests"
  private val renderOpts =
    ConfigRenderOptions.concise()
      .setJson(true)
      .setComments(false)
      .setOriginComments(false)
  val contests: Seq[Contest] =
    read[Seq[Contest]](config.getValue(key).render(renderOpts))
