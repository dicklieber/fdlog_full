package fdswarm.fx.utils

import javafx.collections.FXCollections
import javafx.collections.MapChangeListener

class ObservableScalaMap[K, V]:
  private val delegate = FXCollections.observableHashMap[K, V]()

  def put(k: K, v: V): Unit = delegate.put(k, v)
  def remove(k: K): Unit = delegate.remove(k)

  def onChange(f: (K, Option[V], Option[V]) => Unit): Unit =
    delegate.addListener((c: MapChangeListener.Change[? <: K, ? <: V]) =>
      val oldV = if c.wasRemoved() then Some(c.getValueRemoved) else None
      val newV = if c.wasAdded() then Some(c.getValueAdded) else None
      f(c.getKey, oldV, newV)
    )
