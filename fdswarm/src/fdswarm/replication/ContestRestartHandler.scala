package fdswarm.replication

import fdswarm.fx.contest.ContestConfigManager
import jakarta.inject.{Inject, Singleton}
import scalafx.application.Platform

@Singleton
class ContestRestartHandler @Inject() (
                                        nodeStatusDispatcher: NodeStatusDispatcher,
                                        contestConfigManager: ContestConfigManager
):

  nodeStatusDispatcher.addContestRestartListener(
    newConfig =>
      Platform.runLater {
        contestConfigManager.setConfig(
          newConfig
        )
      }
  )
