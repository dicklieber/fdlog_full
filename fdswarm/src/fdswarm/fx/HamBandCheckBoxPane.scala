package fdswarm.fx

import fdswarm.model.{AvailableBands, HamBand}

import scalafx.Includes.*
import scalafx.beans.property.ObjectProperty
import scalafx.collections.ObservableBuffer
import scalafx.geometry.Insets
import scalafx.scene.control.{CheckBox, ScrollPane, Tooltip}
import scalafx.scene.layout.{Pane, VBox}

final class HamBandCheckBoxPane(
                                 initialSelected: Set[HamBand] = Set.empty,
                                 spacingPx: Double = 6.0
                               ):

  /** Live set of selected bands (updates as user clicks). */
  val selectedBandsProperty: ObjectProperty[Set[HamBand]] =
    ObjectProperty(initialSelected)

  def selectedBands: Set[HamBand] =
    selectedBandsProperty.value

  /** Programmatically set selection and update the UI. */
  def selectedBands_=(v: Set[HamBand]): Unit =
    selectedBandsProperty.value = v
    boxes.foreach { case (band, cb) =>
      cb.selected = v.contains(band)
    }

  /** Convenience conversion to your model. */
  def toAvailableBands: AvailableBands =
    AvailableBands(selectedBands.toSeq.sortBy(_.startFrequencyHz))

  // Build checkboxes
  private lazy val boxes: Seq[(HamBand, CheckBox)] =
    HamBand.all.toSeq.map { band =>
      val cb = new CheckBox(band.bandName)
      cb.selected = initialSelected.contains(band)
      cb.tooltip = new Tooltip(
        s"${band.bandClass}  ${band.startFrequencyHz}–${band.endFrequencyHz} Hz"
      )

      cb.selected.onChange { (_, _, _) =>
        selectedBandsProperty.value =
          boxes.collect { case (b, c) if c.selected.value => b }.toSet
      }

      band -> cb
    }

  // VBox that holds the checkboxes
  private val listBox: VBox =
    val v = new VBox()
    v.delegate.setSpacing(spacingPx)
    v.delegate.setPadding(Insets(8))          // scalafx.Includes provides conversion
    v.children = ObservableBuffer.from(boxes.map(_._2))
    v

  // ScrollPane that wraps the VBox (using JavaFX delegate setters)
  private val scroll: ScrollPane =
    val sp = new ScrollPane()
    sp.delegate.setFitToWidth(true)
    sp.delegate.setContent(listBox.delegate)
    sp

  /** The Pane you can drop into your scene graph. */
  val pane: Pane =
    val root = new VBox()
    root.children = ObservableBuffer(scroll)
    root