/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */

package com.typesafe.trace

import akka.util.Duration
import org.jboss.netty.akka.util.{ TimerTask, Timeout ⇒ HWTimeout }

/**
 * Tracing of scheduled tasks.
 */
class SchedulerTrace(trace: Trace) {

  val schedulerEvents = trace.settings.events.scheduler

  def tracingScheduler = schedulerEvents && trace.tracing

  def newInfo() = TaskInfo(dispatcher = "scheduler")

  def scheduledOnce(info: TaskInfo, delay: Duration): TraceContext = {
    val sampled = trace.sampled
    if (sampled > 0)
      if (schedulerEvents) trace.branch(ScheduledOnce(info, delay.toString), sampled)
      else trace.continue
    else if (trace.within && sampled == 0) TraceContext.NoTrace
    else TraceContext.EmptyTrace
  }

  def started(info: TaskInfo): Unit = {
    if (tracingScheduler) {
      trace.event(ScheduledStarted(info))
    }
  }

  def completed(info: TaskInfo): Unit = {
    if (tracingScheduler) {
      trace.event(ScheduledCompleted(info))
    }
  }

  def cancelled(info: TaskInfo): Unit = {
    if (tracingScheduler) {
      trace.event(ScheduledCancelled(info))
    }
  }
}

class TracedTimerTask(task: TimerTask, tracer: ActorSystemTracer, context: TraceContext, taskInfo: TaskInfo) extends TimerTask with TraceInfo {
  def info: Info = taskInfo

  def run(timeout: HWTimeout): Unit = {
    tracer.trace.local.start(context)
    tracer.scheduler.started(taskInfo)
    try {
      task.run(timeout)
    } finally {
      tracer.scheduler.completed(taskInfo)
      tracer.trace.local.end()
    }
  }
}
