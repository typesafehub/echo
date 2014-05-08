/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */

package com.typesafe.trace

import scala.concurrent.duration.FiniteDuration

/**
 * Tracing of scheduled tasks.
 */
class SchedulerTrace(trace: Trace) {

  val schedulerEvents = trace.settings.events.scheduler

  def tracingScheduler = schedulerEvents && trace.tracing

  def newInfo(name: String) = TaskInfo(dispatcher = name)

  def scheduledOnce(info: TaskInfo, delay: FiniteDuration): TraceContext = {
    val sampled = trace.sampled
    if (sampled > 0)
      if (schedulerEvents) trace.branch(ScheduledOnce(info, delay.toString), sampled)
      else trace.continue
    else if (trace.within && sampled == 0) TraceContext.NoTrace
    else TraceContext.EmptyTrace
  }

  def cancelled(info: TaskInfo): Unit = {
    if (tracingScheduler) {
      trace.event(ScheduledCancelled(info))
    }
  }
}
