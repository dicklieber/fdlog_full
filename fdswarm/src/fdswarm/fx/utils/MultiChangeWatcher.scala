package fdswarm.fx.utils

import scalafx.beans.property.BooleanProperty
import scalafx.beans.value.ObservableValue

object MultiChangeWatcher:

  def apply(props: ObservableValue[?, ?]*): BooleanProperty =
    val dirty = BooleanProperty(false)
    props.foreach(_.onChange { dirty() = !dirty.value })
    dirty

