package fdswarm.fx.utils

import javafx.collections.FXCollections
import javafx.collections.MapChangeListener
import scala.jdk.CollectionConverters.*

class ObservableScalaMap[K, V]:
  private val delegate = FXCollections.observableHashMap[K, V]()

  def put(k: K, v: V): Unit = delegate.put(k, v)
  def remove(k: K): Unit = delegate.remove(k)
  def values: Seq[V] = delegate.values().asScala.toSeq

  def onChange(f: (K, Option[V], Option[V]) => Unit): Unit =
    delegate.addListener(new MapChangeListener[K, V]:
      override def onChanged(c: MapChangeListener.Change[? <: K, ? <: V]): Unit =
        val oldV: Option[V] = if c.wasRemoved() then Some(c.getValueRemoved) else None
        val newV: Option[V] = if c.wasAdded() then Some(c.getValueAdded) else None
        f(c.getKey, oldV, newV)
    )
