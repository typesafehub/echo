/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */

package com.typesafe.atmos.trace

/**
 * Tracing of execution context related events.
 */
class ExecutionContextTrace(trace: Trace) {

  val runnableEvents = trace.settings.events.runnables

  def tracingRunnables = runnableEvents && trace.tracing

  def newTaskInfo(executionContext: String) = TaskInfo(dispatcher = executionContext)

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

/**
 * Runnable marked as being traced and expected to be scheduled.
 * Currently used for Akka 2.2 scheduler tracing.
 */
class TracedUnscheduledRunnable(runnable: Runnable, context: TraceContext, info: TaskInfo) extends Runnable {
  def scheduled(tracer: ExecutionContextTracer): TracedRunnable = {
    tracer.trace.local.start(context)
    val newContext = tracer.executionContext.runnableScheduled(info)
    tracer.trace.local.end()
    new TracedRunnable(runnable, tracer, newContext, info)
  }

  def run: Unit = runnable.run()
}

class TracedRunnable(runnable: Runnable, tracer: ExecutionContextTracer, context: TraceContext, info: TaskInfo) extends Runnable {
  def run: Unit = {
    tracer.trace.local.start(context)
    tracer.executionContext.runnableStarted(info)
    try {
      runnable.run()
    } finally {
      tracer.executionContext.runnableCompleted(info)
      tracer.trace.local.end()
    }
  }
}
