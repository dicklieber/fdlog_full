package fdswarm.fx.bands

import jakarta.inject.Inject
import scalafx.Includes.*
import scalafx.beans.property.{BooleanProperty, ObjectProperty}
import scalafx.collections.ObservableBuffer
import scalafx.geometry.Insets
import scalafx.scene.control.{Button, CheckBox, ScrollPane, Tooltip}
import scalafx.scene.layout.{HBox, Pane, VBox}

final class HamBandCheckBoxPane @Inject() (
                                            store: AvailableBandsStore,
                                            catalog: HamBandCatalog
                                          ):

  private val spacingPx = 6.0

  private val initialSelected: Set[String] =
    store.availableBands.bandNames

  private var savedSnapshot: Set[String] =
    initialSelected

  val selectedNamesProperty: ObjectProperty[Set[String]] =
    ObjectProperty(initialSelected)

  val dirtyProperty: BooleanProperty =
    BooleanProperty(false)

  private def updateDirty(): Unit =
    dirtyProperty.value = selectedNamesProperty.value != savedSnapshot

  // ---- UI ----

  private lazy val boxes: Seq[(HamBand, CheckBox)] =
    catalog.all.map { band =>
      val cb = new CheckBox(band.bandName)
      cb.selected = initialSelected.contains(band.bandName)

      val regionText =
        band.ituRegionAvailability match
          case ItuRegionAvailability.AllRegions        => "All ITU regions"
          case ItuRegionAvailability.VariesByCountry   => "Varies by country"
          case ItuRegionAvailability.RegionsOnly(rs)   => s"Regions: ${rs.mkString(",")}"

      cb.tooltip = new Tooltip(
        s"${band.bandClass}  ${band.startFrequencyHz}–${band.endFrequencyHz} Hz  $regionText"
      )

      cb.selected.onChange { (_, _, _) =>
        val names =
          boxes.collect { case (b, c) if c.selected.value => b.bandName }.toSet
        selectedNamesProperty.value = names
        store.setBandNames(names)
        updateDirty()
      }

      band -> cb
    }

  private val listBox: VBox =
    new VBox {
      spacing = spacingPx
      padding = Insets(8)
      children = ObservableBuffer.from(boxes.map(_._2))
    }

  private val scroll: ScrollPane =
    new ScrollPane {
      fitToWidth = true
      content = listBox
    }

  private val saveButton: Button =
    val b = new Button("Save")
    b.disable <== dirtyProperty.not()
    b.onAction = _ =>
      store.saveNow()
      savedSnapshot = selectedNamesProperty.value
      updateDirty()
    b

  val pane: Pane =
    new VBox {
      children = ObservableBuffer(
        new HBox {
          padding = Insets(8)
          children = ObservableBuffer(saveButton)
        },
        scroll
      )
    }