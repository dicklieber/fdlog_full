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

import com.typesafe.config.{Config, ConfigRenderOptions, ConfigValue}
import io.circe.Codec
import jakarta.inject.{Inject, Singleton}
import io.circe.parser.decode

case class ContestClassChar(
                            ch: String,
                            description: String
                          ) derives Codec.AsObject

case class Contest(
                    name: ContestType,
                    classChars: Seq[ContestClassChar]
                  ) derives Codec.AsObject:
  def isValidClass(classChar: String): Boolean =
    classChars.exists(_.ch == classChar)

@Singleton
final class ContestCatalog @Inject()(config: Config):
  /** Render the *list* at fdswarm.contests as strict JSON and decode */
  private val key = "fdswarm.contests"
  private val renderOpts =
    ConfigRenderOptions.concise()
      .setJson(true)
      .setComments(false)
      .setOriginComments(false)
  private val configValue: ConfigValue = config.getValue(key)
  val contests: Seq[Contest] =
    decode[Seq[Contest]](configValue.render(renderOpts)).toTry.get
