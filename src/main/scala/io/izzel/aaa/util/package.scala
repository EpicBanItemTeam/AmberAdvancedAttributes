package io.izzel.aaa

import org.spongepowered.api.Sponge
import org.spongepowered.api.event.{Event, EventListener}
import org.spongepowered.api.plugin.PluginContainer

import scala.collection.mutable
import scala.reflect.{ClassTag, classTag}
import scala.util.continuations.{shift, suspendable}

package object util {
  private val handlers: mutable.Map[Class[_ <: Event], mutable.Queue[(Event => Boolean, Event => Unit)]] = mutable.Map()

  private class ScalaEventListener[E <: Event: ClassTag](handler: E => Unit) extends EventListener[E] {
    override def handle(event: E): Unit = handler(event)
  }

  def waitFor[E <: Event : ClassTag](implicit container: PluginContainer): E@suspendable = waitFor((_: E) => true)

  def waitFor[E <: Event : ClassTag](filter: E => Boolean)(implicit container: PluginContainer): E@suspendable = {
    shift { (continue: E => Unit) =>
      val eventClass = classTag[E].runtimeClass.asInstanceOf[Class[E]]
      val queue: mutable.Queue[(Event => Boolean, Event => Unit)] = handlers.getOrElseUpdate(eventClass, {
        val queue = mutable.Queue[(Event => Boolean, Event => Unit)]()
        val handler = (event: E) => queue.dequeueAll(_._1(event)).foreach(_._2(event))
        Sponge.getEventManager.registerListener(container, eventClass, new ScalaEventListener[E](handler))
        queue
      })
      queue.enqueue((e => filter(eventClass.cast(e)), e => continue(eventClass.cast(e))))
    }
  }

  def listenTo[E <: Event : ClassTag](handler: E => Unit)(implicit container: PluginContainer): () => Unit = {
    val eventListener = new ScalaEventListener[E](handler)
    val eventClass = classTag[E].runtimeClass.asInstanceOf[Class[E]]
    Sponge.getEventManager.registerListener(container, eventClass, eventListener)
    val unsubscribeHandler = () => Sponge.getEventManager.unregisterListeners(eventListener)
    unsubscribeHandler
  }
}
