package fdswarm.fx.bands

import fdswarm.model.BandMode.Band
import jakarta.inject.{Inject, Singleton}
import scalafx.scene.Node
import scalafx.scene.control.{CheckBox, Label, TitledPane, Tooltip}
import scalafx.scene.layout.GridPane

@Singleton
final class BandCheckBoxPane @Inject()(
                                        availableBandsManager: AvailableBandsManager,
                                        hamBandCatalog: BandCatalog
                                      ):

  val checkBoxes: Seq[BandCheckBox] =
    hamBandCatalog.hamBands.map { hamBand =>
      BandCheckBox(hamBand)
    }
  private val grid = new GridPane {
    hgap = 12.0
    vgap = 6.0
  }
  val node: Node =
    new TitledPane {
      content = grid
      text = "Ham bands"
      collapsible = false
    }
  private val byBandClass: Map[BandClass, Seq[BandCheckBox]] =
    checkBoxes.groupBy(_.hamBand.bandClass)


  for
    (bandClass, row) <- BandClass.values.zipWithIndex
    if byBandClass.contains(bandClass)
    _ = grid.addRow(row, new Label(bandClass.toString))
    (bandCheckBox, col) <- byBandClass(bandClass).zipWithIndex
  do
    grid.add(bandCheckBox, col + 1, row)

  def setChecks(): Unit = checkBoxes
    .foreach(bandCheckBox =>
      bandCheckBox.selected.value =
        availableBandsManager.bands.contains(bandCheckBox.bandName))
  setChecks()//todo

  private def checked: Seq[Band] =
    checkBoxes.filter(_.selected.value).map(_.bandName)

  case class BandCheckBox(hamBand: HamBand) extends CheckBox {
    text = hamBand.bandName
    selected = availableBandsManager.bands.contains(hamBand.bandName)
    tooltip = new Tooltip(
      s"${hamBand.bandClass}  ${hamBand.startFrequencyHz}–${hamBand.endFrequencyHz} Hz"
    )
    selected.onChange { (_, _, _) =>
      availableBandsManager.save(checked)
    }
    val bandName: String = hamBand.bandName
  }