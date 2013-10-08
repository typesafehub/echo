/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */

package com.typesafe.atmos.trace

import akka.actor.{ ActorRef, Props }
import akka.dispatch.MessageDispatcher

/**
 * Tracing of dispatchers and dispatcher related events.
 */
class DispatcherTrace(trace: Trace) {

  val runnableEvents = trace.settings.events.runnables

  def tracingRunnables = runnableEvents && trace.tracing

  val monitor: Option[ActorRef] = {
    if (trace.settings.useDispatcherMonitor)
      Some(AtmosSystem.create(Props(new DispatcherMonitor(trace)).withDispatcher(Trace.DispatcherId), "DispatcherMonitor"))
    else None
  }

  def started(dispatcher: MessageDispatcher): Unit = {
    monitor foreach { _ ! DispatcherMonitor.DispatcherStart(dispatcher) }
  }

  def shutdown(dispatcher: MessageDispatcher): Unit = {
    monitor foreach { _ ! DispatcherMonitor.DispatcherShutdown(dispatcher) }
  }

  def newTaskInfo(dispatcher: String) = TaskInfo(dispatcher = dispatcher)

  def runnableScheduled(info: TaskInfo): TraceContext = {
    val sampled = trace.sampled
    if (sampled > 0)
      if (runnableEvents) trace.branch(RunnableScheduled(info), sampled)
      else trace.continue
    else if (trace.within && sampled == 0) TraceContext.NoTrace
    else TraceContext.EmptyTrace
  }

  def runnableStarted(info: TaskInfo): Unit = {
    if (tracingRunnables) {
      trace.event(RunnableStarted(info))
    }
  }

  def runnableCompleted(info: TaskInfo): Unit = {
    if (tracingRunnables) {
      trace.event(RunnableCompleted(info))
    }
  }
}
