package fdswarm.replication

import fdswarm.fx.contest.{ContestConfig, ContestConfigManager}
import jakarta.inject.{Inject, Singleton}
import scalafx.application.Platform

@Singleton
class ContestRestartHandler @Inject() (
                                        nodeStatusDispatcher: NodeStatusDispatcher,
                                        contestConfigManager: ContestConfigManager
):

  nodeStatusDispatcher.addContestRestartListener(
    listener = handleContestConfigUpdate
  )

  nodeStatusDispatcher.addSyncContestListener(
    listener = handleContestConfigUpdate
  )

  private def handleContestConfigUpdate(
    newConfig: ContestConfig
  ): Unit =
    Platform.runLater {
      contestConfigManager.setConfig(
        newConfig
      )
    }
