/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */

package com.typesafe.atmos.trace

import akka.dispatch.Future
import akka.util.Duration
import com.typesafe.atmos.util.Uuid
import java.util.concurrent.TimeoutException

object FutureTrace {

  val ZeroFutureInfo = FutureInfo(Uuid.zero)

  private val sampledAndInfo = new ThreadLocal[(Int, FutureInfo)]

  def getSampledAndInfo: (Int, FutureInfo) = sampledAndInfo.get

  def sampled: Int = {
    val current = getSampledAndInfo
    if (current == null) -1 else current._1
  }

  def info: FutureInfo = {
    val current = getSampledAndInfo
    if (current == null) ZeroFutureInfo else current._2
  }

  def store(sampled: Int, info: FutureInfo): Unit = {
    sampledAndInfo.set((sampled, info))
  }

  def clear(): Unit = {
    sampledAndInfo.set(null)
  }
}

object TracedFutureInfo {
  def apply(future: Future[_]): FutureInfo = TraceInfo(future) match {
    case info: FutureInfo ⇒ info
    case _                ⇒ FutureTrace.ZeroFutureInfo
  }
}

/**
 * Tracing events of Futures.
 */
class FutureTrace(trace: Trace) {
  import FutureTrace._

  val futureEvents = trace.settings.events.futures

  def tracingFutures = futureEvents && trace.tracing

  def newInfo(dispatcher: String): FutureInfo = FutureInfo(dispatcher = dispatcher)

  def created(info: FutureInfo): Unit = {
    if (tracingFutures) {
      trace.event(FutureCreated(info))
    }
  }

  def scheduled(info: FutureInfo, taskInfo: TaskInfo): TraceContext = {
    val sampled = sample()
    if (sampled > 0)
      if (futureEvents) trace.branch(FutureScheduled(info, taskInfo), sampled)
      else trace.continue
    else if (trace.within && sampled == 0) TraceContext.NoTrace
    else TraceContext.EmptyTrace
  }

  def completed[T](info: FutureInfo, value: Either[Throwable, T]): Unit = {
    if (tracingFutures) {
      value match {
        case Right(result)   ⇒ trace.event(FutureSucceeded(info, trace.formatLongMessage(result), TraceInfo(result)))
        case Left(exception) ⇒ trace.event(FutureFailed(info, trace.formatLongException(exception)))
      }
    }
  }

  def callbackAdded(info: FutureInfo): TraceContext = {
    val sampled = sample()
    if (sampled > 0)
      if (futureEvents) trace.branch(FutureCallbackAdded(info), sampled)
      else trace.continue
    else if (trace.within && sampled == 0) TraceContext.NoTrace
    else TraceContext.EmptyTrace
  }

  def callbackStarted(info: FutureInfo): Unit = {
    if (tracingFutures) {
      trace.event(FutureCallbackStarted(info))
    }
  }

  def callbackCompleted(info: FutureInfo): Unit = {
    if (tracingFutures) {
      trace.event(FutureCallbackCompleted(info))
    }
  }

  def awaited(info: FutureInfo): Unit = {
    if (tracingFutures) {
      trace.event(FutureAwaited(info))
    }
  }

  def timedOut(info: FutureInfo, duration: Duration, exception: TimeoutException): Unit = {
    if (tracingFutures) {
      trace.event(FutureAwaitTimedOut(info, duration.toString))
    }
    // rethrow the exception from here
    throw exception
  }

  def sample(): Int = if (trace.within) trace.sampled else 1
}

class FutureCallback[T](func: Either[Throwable, T] ⇒ Unit, tracer: ActorSystemTracer, context: TraceContext, futureInfo: FutureInfo)
  extends Function1[Either[Throwable, T], Unit] with TraceInfo {

  def info: Info = futureInfo

  def apply(value: Either[Throwable, T]): Unit = {
    tracer.trace.local.start(context)
    tracer.future.callbackStarted(futureInfo)
    try {
      func(value)
    } finally {
      tracer.future.callbackCompleted(futureInfo)
      tracer.trace.local.end()
    }
  }
}
