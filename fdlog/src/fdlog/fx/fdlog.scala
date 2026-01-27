package fdlog.fx

import com.google.inject.{Guice, Injector}
import com.typesafe.scalalogging.LazyLogging

import scalafx.application.{JFXApp3, Platform}
import scalafx.scene.Scene
import scalafx.scene.layout._
import scalafx.scene.control._
import scalafx.geometry.{Insets, Pos}
import scalafx.beans.property._
import scalafx.collections.ObservableBuffer
import scalafx.scene.input.{KeyCode, KeyEvent}
import scalafx.Includes._

object fdlog extends  JFXApp3 with LazyLogging:
  logger.info("fdlog ctor")
  val injector: Injector = Guice.createInjector(new ConfigModule())

  
  
  override def start(): Unit =
    logger.info("start...")

    // Layout for form: grid
    val formGrid = new GridPane {
      hgap = 8
      vgap = 4
      padding = Insets(6)

      add(new Label("My Call:"), 2, 1);
      add(myCallField, 3, 1)
      add(new Label("Their Call:"), 0, 2);
      add(theirCallField, 1, 2)
      add(new Label("Exch Rcvd:"), 0, 4);
      add(exchRcvdField, 1, 4)
      add(new Label("Operator:"), 2, 4);
//      add(operatorField, 3, 4)

      add(new Label("Notes:"), 0, 5)
//      add(notesArea, 1, 5, 3, 1) // span columns
    }


    val myCallUp = myCallField.text.value.trim.toUpperCase
    val theirUp = theirCallField.text.value.trim.toUpperCase
    val exchRUp = exchRcvdField.text.value.trim.toUpperCase

    final case class QsoInput(
                               band: String,
                               mode: String,
                               myCall: String,
                               theirCall: String,
                               exchangeSent: String,
                               exchangeRcvd: String,
                               operator: Option[String],
                               notes: Option[String]
                             )


    // Form fields
    val bandField = new TextField {
      text = "40m"
    }
    val modeField = new TextField {
      text = "CW"
    }
    val freqField = new TextField {
      text = "7100000"
    }
    val myCallField = new TextField
    val theirCallField = new TextField
    val rstSentField = new TextField {
      text = "599"
    }
    val rstRcvdField = new TextField {
      text = "599"
    }

    val statusLabel = new Label("Ready.")


    // Row wrapper for binding into TableView
    final case class QsoRow(
                             time: StringProperty,
                             band: StringProperty,
                             mode: StringProperty,
                             myCall: StringProperty,
                             theirCall: StringProperty,
                             exchS: StringProperty,
                             exchR: StringProperty
                           )

    val qsoBuffer = ObservableBuffer[QsoRow]()

    // Table of QSOs
    val table = new TableView[QsoRow](qsoBuffer) {
      columns ++= List(
        new TableColumn[QsoRow, String]("Time") {
          cellValueFactory = _.value.time
          prefWidth = 180
        },
        new TableColumn[QsoRow, String]("Band") {
          cellValueFactory = _.value.band
          prefWidth = 70
        },
        new TableColumn[QsoRow, String]("Mode") {
          cellValueFactory = _.value.mode
          prefWidth = 70
        },
        new TableColumn[QsoRow, String]("My Call") {
          cellValueFactory = _.value.myCall
          prefWidth = 100
        },
        new TableColumn[QsoRow, String]("Their Call") {
          cellValueFactory = _.value.theirCall
          prefWidth = 110
        },
        new TableColumn[QsoRow, String]("Exch Sent") {
          cellValueFactory = _.value.exchS
          prefWidth = 100
        },
        new TableColumn[QsoRow, String]("Exch Rcvd") {
          cellValueFactory = _.value.exchR
          prefWidth = 100
        }
      )
    }




    // Root layout + GLOBAL ENTER HANDLER
    stage = new JFXApp3.PrimaryStage {
      title = "FDLog (ScalaFX)"
      width = 1100
      height = 650

      scene = new Scene {
        // Global ENTER → save, except when in Notes
        onKeyPressed = (e: KeyEvent) =>
          if e.code == KeyCode.Enter && !e.shiftDown then
//            if !notesArea.isFocused then
//              saveQso()
              e.consume()

        val bottomBox = new VBox {
          spacing = 4
          children = Seq(
//            presetBar, // presets row
            formGrid,
//            buttonBar,
            statusLabel
          )
        }


        root = new BorderPane {
          center = new VBox {
            padding = Insets(8)
            spacing = 4
            children = Seq(
              new Label("QSOs") {
                style = "-fx-font-size: 16px; -fx-font-weight: bold;"
              },
              table
            )
          }
          bottom = bottomBox
        }
      }
    }


