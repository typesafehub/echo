/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */

package com.typesafe.trace;

import com.typesafe.trace.IterateeTrace.StepTracerHook;
import java.lang.System;
import play.api.libs.iteratee.Iteratee;
import scala.Function1;
import scala.concurrent.ExecutionContext;

privileged aspect IterateeTraceAspect {

  // add trace metadata to Iteratee

  declare parents: Iteratee implements IterateeTag;

  private volatile IterateeTagData Iteratee._atmos$tag;

  public IterateeTagData Iteratee.atmos$tag() {
    return _atmos$tag;
  }

  public void Iteratee.atmos$tag(IterateeTagData tag) {
    _atmos$tag = tag;
  }

  public void Iteratee.atmos$setChunking() {
    if (this._atmos$tag == null) {
      this.atmos$tag(IterateeTrace.Chunking());
    }
  }

  public boolean Iteratee.atmos$chunking() {
    if (this._atmos$tag != null) {
      return this.atmos$tag().chunking();
    } else return false;
  }

  public IterateeTagData Iteratee.tag() {
    if (this._atmos$tag == null) {
      this.atmos$tag(IterateeTrace.ZeroIterateeTagData());
      return this._atmos$tag;
    } else return this._atmos$tag;
  }

  public boolean enabled(ActionTracer tracer) {
    return tracer != null && tracer.enabled();
  }

  public boolean disabled(ActionTracer tracer) {
    return tracer == null || !tracer.enabled();
  }

  public boolean tracing(ActionTracer tracer) {
    return enabled(tracer) && tracer.trace().tracing();
  }

  declare parents: Iteratee implements TraceInfo;

  // info

  private volatile IterateeInfo Iteratee._atmos$info;

  public IterateeInfo Iteratee.atmos$info() {
    if (_atmos$info == null) return IterateeTrace.ZeroIterateeInfo();
    else return _atmos$info;
  }

  public void Iteratee.atmos$info(IterateeInfo info) {
    _atmos$info = info;
  }

  public Info Iteratee.info() {
    return (Info) this._atmos$info;
  }

  before(Iteratee iteratee):
    execution(play.api.libs.iteratee.Iteratee+.new(..)) &&
    this(iteratee)
  {
    ActionTracer tracer = ActionTracer.global();
    if (tracing(tracer)) {
      IterateeInfo info = IterateeTrace.newInfo();
      iteratee.atmos$info(info);
      tracer.iteratee().created(info);
    }
  }

  // public abstract scala.concurrent.Future fold(scala.Function1, scala.concurrent.ExecutionContext);
  Object around(Iteratee iteratee, Function1 folder, ExecutionContext ctx):
    execution(* play.api.libs.iteratee.Iteratee+.fold(Function1, ExecutionContext)) &&
    this(iteratee) &&
    args(folder, ctx)
  {
    ActionTracer tracer = ActionTracer.global();
    if (tracing(tracer)) {
      tracer.iteratee().folded(iteratee.atmos$info());
      boolean chunking = iteratee.atmos$chunking();
      return proceed(iteratee, new StepTracerHook(tracer, iteratee.atmos$info(), chunking, folder), ctx);
    } else {
      return proceed(iteratee, folder, ctx);
    }
  }

}
