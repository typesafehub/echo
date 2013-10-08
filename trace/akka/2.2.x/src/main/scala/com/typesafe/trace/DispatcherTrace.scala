/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */

package com.typesafe.trace

import akka.actor.{ ActorRef, Props }
import akka.dispatch.MessageDispatcher

/**
 * Tracing of dispatchers and dispatcher related events.
 */
class DispatcherTrace(trace: Trace) {

  val monitor: Option[ActorRef] = {
    if (trace.settings.useDispatcherMonitor)
      Some(InternalSystem.create(Props(new DispatcherMonitor(trace)).withDispatcher(Trace.DispatcherId), "DispatcherMonitor"))
    else None
  }

  def started(dispatcher: MessageDispatcher): Unit = {
    monitor foreach { _ ! DispatcherMonitor.DispatcherStart(dispatcher) }
  }

  def shutdown(dispatcher: MessageDispatcher): Unit = {
    monitor foreach { _ ! DispatcherMonitor.DispatcherShutdown(dispatcher) }
  }
}
