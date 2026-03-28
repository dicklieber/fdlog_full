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
import fdswarm.fx.utils.editor.CustomFieldEditor
import fdswarm.model.Choice
import io.circe.Codec
import jakarta.inject.{Inject, Singleton}
import io.circe.parser.decode
import scalafx.Includes.*
import scalafx.beans.property.{ObjectProperty, Property, StringProperty}
import scalafx.collections.ObservableBuffer
import scalafx.scene.Node
import scalafx.scene.control.{ComboBox, ListCell, ListView}

case class ClassChoice(
                            ch: String,
                            description: String
                          ) extends Choice[Char] derives Codec.AsObject:
  override val value: Char = ch.head
  override val label: String = s"$ch - $description"

/**
 * As defined in [[application.conf]]
 * @param contestType [[WFD]] or [[ARRL]]
 * @param classChoices what classes are allowed in this contest
 */
case class ContestDefinition(
                              name: ContestType,
                              classChoices: Seq[ClassChoice]
                  ) derives Codec.AsObject:
  def isValidClass(classChar: String): Boolean =
    classChoices.exists(_.ch == classChar)
  def classCharsString: String = classChoices.map(_.ch).mkString

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
  val contests: Seq[ContestDefinition] =
    decode[Seq[ContestDefinition]](configValue.render(renderOpts)).toTry.get

  def getContest(contestType: ContestType): Option[ContestDefinition] =
    contests.find(_.name == contestType)


  def comboBox(contestTypeProperty: ObjectProperty[ContestType]): ClassComboBox =
    new ClassComboBox(this, contestTypeProperty)




