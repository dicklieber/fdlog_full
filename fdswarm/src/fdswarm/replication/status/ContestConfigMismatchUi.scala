package fdswarm.replication.status

import fdswarm.fx.utils.{IconButton, StyledDialog}
import fdswarm.replication.{Service, Transport}
import io.circe.syntax.EncoderOps
import jakarta.inject.{Inject, Singleton}
import scalafx.Includes.*
import scalafx.collections.ObservableBuffer
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.control.{Button, ButtonType, ComboBox, Label}
import scalafx.scene.layout.{HBox, Priority, VBox}
import scalafx.scene.paint.Color

import java.nio.charset.StandardCharsets

@Singleton
class ContestConfigMismatchUi @Inject() (
    swarmData: SwarmData,
    transport: Transport
):

  def warningButton(size: Double = 18): Button =
    val button = IconButton(
      name = "exclamation-triangle",
      size = size,
      tooltipText = "Not all nodes have the same Contest Configuration",
      color = Color.Red
    )
    button.visible <== swarmData.contestConfigMismatchProperty
    button.managed <== button.visible
    button.onAction = _ => showDialog()
    button

  private def showDialog(): Unit =
    val allStatuses = swarmData
      .allNodeStatuses
      .sorted
    if allStatuses.isEmpty then return

    val options = allStatuses.map(
      nodeStatus => nodeStatus.nodeIdentity
    )
    val optionLabels = options.map(
      nodeIdentity => s"${nodeIdentity.hostName} (${nodeIdentity.instanceId})"
    )
    val optionByLabel = optionLabels.zip(options).toMap
    val sourceNodeSelector = new ComboBox[String](
      ObservableBuffer.from(
        optionLabels
      )
    ):
      value = optionLabels.headOption.orNull
      hgrow = Priority.Always

    val syncButton = new Button("Broadcast Selected Contest Config"):
      onAction = _ =>
        Option(sourceNodeSelector.value.value)
          .flatMap(
            label => optionByLabel.get(label)
          )
          .flatMap(
            nodeIdentity =>
              swarmData.nodeMap
                .get(nodeIdentity)
                .map(
                  nodeStatus => nodeStatus.statusMessage.contestConfig
                )
          )
          .foreach(
            contestConfig =>
              val payload = contestConfig
                .asJson
                .noSpaces
                .getBytes(
                  StandardCharsets.UTF_8
                )
              transport.send(
                service = Service.SyncContest,
                data = payload
              )
          )

    val dialog = new StyledDialog[ButtonType]:
      title = "Contest Configuration Mismatch"
      headerText = "Not all nodes have the same Contest Configuration"
      resizable = true
    dialog.dialogPane().buttonTypes = Seq(
      ButtonType.Close
    )
    dialog.dialogPane().content = new VBox:
      spacing = 8
      padding = Insets(10)
      children = Seq(
        swarmData.buildGridPane(
          fields = Seq(
            NodeDataField.HostName,
            NodeDataField.ContestType,
            NodeDataField.ContestCallsign,
            NodeDataField.Exchange
          )
        ),
        new HBox:
          spacing = 8
          alignment = Pos.CenterRight
          children = Seq(
            new Label("Source Node"),
            sourceNodeSelector,
            syncButton
          )
      )
    dialog.showAndWait()
