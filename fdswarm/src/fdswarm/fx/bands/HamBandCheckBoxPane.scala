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

package fdswarm.fx.bands

import fdswarm.model.{AvailableBands, HamBand}
import jakarta.inject.{Inject, Named}
import scalafx.Includes.*
import scalafx.beans.property.{BooleanProperty, ObjectProperty}
import scalafx.collections.ObservableBuffer
import scalafx.geometry.Insets
import scalafx.scene.control.{Button, CheckBox, ScrollPane, Tooltip}
import scalafx.scene.layout.{HBox, Pane, VBox}

final class HamBandCheckBoxPane @Inject() (
                                            bandsStore: AvailableBandsStore,
                                          ):
  private val spacingPx: Double = 6.0
  private val initialSelected: Set[HamBand] =
    bandsStore.availableBands.bands

  /** What was last saved/loaded (used to compute "dirty"). */
  private var savedSnapshot: Set[HamBand] =
    initialSelected

  /** Current selection (live). */
  val selectedBandsProperty: ObjectProperty[Set[HamBand]] =
    ObjectProperty(initialSelected)

  def selectedBands: Set[HamBand] =
    selectedBandsProperty.value

  /** Dirty flag: true when current selection differs from savedSnapshot. */
  val dirtyProperty: BooleanProperty =
    BooleanProperty(selectedBands != savedSnapshot)

  def isDirty: Boolean =
    dirtyProperty.value

  private def updateDirty(): Unit =
    dirtyProperty.value = (selectedBands != savedSnapshot)

  def toAvailableBands: AvailableBands =
    AvailableBands(selectedBands)

  // --- UI controls ---
  private lazy val boxes: Seq[(HamBand, CheckBox)] =
    HamBand.all.toSeq.map { band =>
      val cb = new CheckBox(band.bandName)
      cb.selected = initialSelected.contains(band)
      cb.tooltip = new Tooltip(
        s"${band.bandClass}  ${band.startFrequencyHz}–${band.endFrequencyHz} Hz"
      )

      cb.selected.onChange { (_, _, _) =>
        val v = boxes.collect { case (b, c) if c.selected.value => b }.toSet
        selectedBandsProperty.value = v
        bandsStore.setSelectedBands(v) // memory only
        updateDirty()
      }

      band -> cb
    }

  private val listBox: VBox =
    val v = new VBox()
    v.delegate.setSpacing(spacingPx)
    v.delegate.setPadding(Insets(8))
    v.children = ObservableBuffer.from(boxes.map(_._2))
    v

  private val scroll: ScrollPane =
    val sp = new ScrollPane()
    sp.delegate.setFitToWidth(true)
    sp.delegate.setContent(listBox.delegate)
    sp

  private val saveButton: Button =
    val b = new Button("Save")

    // Save enabled only when dirty
    b.disable <== dirtyProperty.not()

    b.onAction = _ =>
      bandsStore.saveNow()
      savedSnapshot = selectedBands
      updateDirty()

    b

  private val topBar: HBox =
    val hb = new HBox()
    hb.padding = Insets(8)
    hb.children = ObservableBuffer(saveButton)
    hb

  /** Pane exposed via AvailableBandsStore */
  val pane: Pane =
    val root = new VBox()
    root.children = ObservableBuffer(topBar, scroll)
    root
