/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */

package com.typesafe.trace;

import java.util.concurrent.TimeoutException;
import scala.concurrent.CanAwait;
import scala.concurrent.duration.Duration;
import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;
import scala.concurrent.impl.Future.PromiseCompletingRunnable;
import scala.Function1;
import scala.util.Try;

privileged aspect FutureTraceAspect {

  public boolean enabled(ExecutionContextTracer tracer) {
    return tracer != null && tracer.enabled();
  }

  public boolean disabled(ExecutionContextTracer tracer) {
    return tracer == null || !tracer.enabled();
  }

  // ----------------------------------------------------
  // Future tracing
  // ----------------------------------------------------

  // add trace metadata to future

  declare parents: Future implements TraceInfo;

  // sampled

  private volatile int Future._atmos$sampled;

  public int Future.atmos$sampled() {
    return _atmos$sampled;
  }

  public void Future.atmos$sampled(int sampled) {
    _atmos$sampled = sampled;
  }

  public boolean Future.atmos$traceable() {
    return _atmos$sampled > 0;
  }

  // info

  private volatile FutureInfo Future._atmos$info;

  public FutureInfo Future.atmos$info() {
    if (_atmos$info == null) return FutureTrace.ZeroFutureInfo();
    else return _atmos$info;
  }

  public void Future.atmos$info(FutureInfo info) {
    _atmos$info = info;
  }

  public Info Future.info() {
    return (Info) this._atmos$info;
  }

  // attach trace metadata to future

  before(Future future):
    execution(scala.concurrent.Future+.new()) &&
    this(future)
  {
    ExecutionContextTracer tracer = ExecutionContextTracer.global();
    if (enabled(tracer)) {
      int sampled = -1; // default to empty trace
      if (tracer.trace().within())
        sampled = tracer.trace().sampled();
      else if (tracer.trace().settings().zeroContextFutures())
        sampled = 1;
      future.atmos$sampled(sampled);
      if (sampled > 0) {
        FutureInfo info = tracer.future().newInfo("execution-context");
        future.atmos$info(info);
        tracer.future().created(info);
      }
    }
  }

  before(Future future, Try value):
    execution(scala.concurrent.Future+.new(..)) &&
    this(future) &&
    args(value)
  {
    ExecutionContextTracer tracer = ExecutionContextTracer.global();
    if (enabled(tracer)) {
      int sampled = -1; // default to empty trace
      if (tracer.trace().within())
        sampled = tracer.trace().sampled();
      else if (tracer.trace().settings().zeroContextFutures())
        sampled = 1;
      future.atmos$sampled(sampled);
      if (sampled > 0) {
        FutureInfo info = tracer.future().newInfo("execution-context");
        future.atmos$info(info);
        tracer.future().created(info);
        tracer.future().completed(info, value);
      }
    }
  }

  // future apply

  Object around(ExecutionContext executor, Runnable runnable):
    execution(* scala.concurrent.ExecutionContext+.execute(..)) &&
    this(executor) &&
    args(runnable)
  {
    ExecutionContextTracer tracer = ExecutionContextTracer.global();
    if (disabled(tracer)) return proceed(executor, runnable);
    // futures are scheduled with a PromiseCompletingRunnable
    if (runnable instanceof PromiseCompletingRunnable) {
      Future future = ((PromiseCompletingRunnable) runnable).promise().future();
      int sampled = future.atmos$sampled();
      FutureInfo futureInfo = future.atmos$info();
      TaskInfo taskInfo = tracer.future().newTaskInfo("execution-context");
      TraceContext context = TraceContext.EmptyTrace();
      if (sampled == 0) {
        context = TraceContext.NoTrace();
      } else if (sampled > 0) {
        context = tracer.future().scheduled(futureInfo, taskInfo);
      }
      FutureRunnable futureRunnable = new FutureRunnable(runnable, tracer, context, taskInfo);
      return proceed(executor, futureRunnable);
    } else if (runnable instanceof TracedUnscheduledRunnable) {
      TracedRunnable tracedRunnable = ((TracedUnscheduledRunnable) runnable).scheduled(tracer);
      return proceed(executor, tracedRunnable);
    } else {
      return proceed(executor, runnable);
    }
  }

  // future completion

  boolean around(Future future, Try value):
    execution(* scala.concurrent.Promise+.tryComplete(..)) &&
    this(future) &&
    args(value)
  {
    ExecutionContextTracer tracer = ExecutionContextTracer.global();
    boolean completed = proceed(future, value);
    if (completed && enabled(tracer) && future.atmos$traceable()) {
      tracer.future().completed(future.atmos$info(), value);
    }
    return completed;
  }

  // trace future callbacks - add metadata to wrapped function

  Object around(Future future, Function1 func, ExecutionContext executor):
    execution(* scala.concurrent.Future+.onComplete(..)) &&
    this(future) &&
    args(func, executor)
  {
    ExecutionContextTracer tracer = ExecutionContextTracer.global();
    if (disabled(tracer)) return proceed(future, func, executor);
    int sampled = future.atmos$sampled();
    FutureInfo info = future.atmos$info();
    TraceContext context = TraceContext.EmptyTrace();
    if (sampled == 0) {
        context = TraceContext.NoTrace();
    } else if (sampled > 0) {
      context = tracer.future().callbackAdded(info);
    }
    FutureCallback callback = new FutureCallback(func, tracer, context, info);
    return proceed(future, callback, executor);
  }

  Object around(Future future, Duration duration, CanAwait permit):
    execution(* scala.concurrent.Future+.ready(..)) &&
    this(future) &&
    args(duration, permit)
  {
    ExecutionContextTracer tracer = ExecutionContextTracer.global();
    if (disabled(tracer) || !future.atmos$traceable()) return proceed(future, duration, permit);
    try {
      Object result = proceed(future, duration, permit);
      tracer.future().awaited(future.atmos$info());
      return result;
    } catch (TimeoutException exception) {
      // exception is rethrown in timedOut
      tracer.future().timedOut(future.atmos$info(), duration, exception);
      return null;
    }
  }

}
