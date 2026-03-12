package fdswarm.fx.utils

import scalafx.beans.property.BooleanProperty
import scalafx.beans.value.ObservableValue

object FormDirtyTracker:

  def watch(props: ObservableValue[?, ?]*): BooleanProperty =
    val dirty = BooleanProperty(false)
    props.foreach(_.onChange { dirty() = true })
    dirty

  def reset(dirty: BooleanProperty): Unit =
    dirty() = false