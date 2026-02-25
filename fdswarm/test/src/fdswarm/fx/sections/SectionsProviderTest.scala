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

import com.typesafe.config.ConfigFactory
import fdswarm.JavaFxTestKit
import munit.FunSuite
import scalafx.Includes.*
import scalafx.beans.binding.Bindings

class SectionsProviderTest extends FunSuite:

  override def beforeAll(): Unit =
    JavaFxTestKit.init()

  test("SectionsProvider provides a Seq[SectionGroup] as a val") {
    val config = ConfigFactory.load()
    val provider = new SectionsProvider(config)
    
    val groups = provider.sectionGroups
    assert(groups.nonEmpty)
    
    val usGroup = groups.find(_.name == "US")
    assert(usGroup.isDefined)
    assert(usGroup.get.sections.exists(_.code == "IL"))
    assertEquals(usGroup.get.sections.size, 66)
    
    val caGroup = groups.find(_.name == "CA")
    assert(caGroup.isDefined)
    assert(caGroup.get.sections.exists(_.code == "QC"))
    
    val dxGroup = groups.find(_.name == "DX")
    assert(dxGroup.isDefined)
    assert(dxGroup.get.sections.exists(_.code == "DX"))

    val allSections = provider.allSections
    assert(allSections.nonEmpty)
    assertEquals(allSections.size, groups.map(_.sections.size).sum)
    assert(allSections.exists(_.code == "IL"))
    assert(allSections.exists(_.code == "QC"))
    assert(allSections.exists(_.code == "DX"))
  }

  test("Section properties are correctly initialized") {
    JavaFxTestKit.init()
    val section = Section("IL", "Illinois")
    assertEquals(section.text.value, "IL")
    assert(section.tooltip.value.isInstanceOf[javafx.scene.control.Tooltip])
    assertEquals(section.tooltip.value.text.value, "Illinois")
  }

  test("Section selection updates StringProperty and respects canSubmit binding") {
    val section = Section("IL", "Illinois")
    val prop = scalafx.beans.property.StringProperty("")
    var called = false

    // Test with canSubmit = true
    val canSubmitTrue = Bindings.createBooleanBinding(() => true)
    section.onSelect(prop, () => called = true, canSubmitTrue)
    section.onMouseClicked.value.handle(null)
    assertEquals(prop.value, "IL")
    assert(called)

    // Test with canSubmit = false
    prop.value = ""
    called = false
    val canSubmitFalse = Bindings.createBooleanBinding(() => false)
    section.onSelect(prop, () => called = true, canSubmitFalse)
    section.onMouseClicked.value.handle(null)
    assertEquals(prop.value, "") // Value should NOT change when disabled
    assert(!called) // And onSelected should NOT be called

    // Test toggling canSubmit from false to true
    prop.value = ""
    called = false
    val backing = scalafx.beans.property.BooleanProperty(false)
    val canSubmitToggle = Bindings.createBooleanBinding(() => backing.value, backing)
    section.onSelect(prop, () => called = true, canSubmitToggle)
    // Initially false: click should not call onSelected
    section.onMouseClicked.value.handle(null)
    assertEquals(prop.value, "")
    assert(!called)
    // Now enable and click again
    prop.value = ""
    backing.value = true
    section.onMouseClicked.value.handle(null)
    assertEquals(prop.value, "IL")
    assert(called)
  }
