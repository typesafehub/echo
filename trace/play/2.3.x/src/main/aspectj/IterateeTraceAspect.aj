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

  private volatile IterateeTagData Iteratee._echo$tag;

  public IterateeTagData Iteratee.echo$tag() {
    return _echo$tag;
  }

  public void Iteratee.echo$tag(IterateeTagData tag) {
    _echo$tag = tag;
  }

  public void Iteratee.echo$setChunking() {
    if (this._echo$tag == null) {
      this.echo$tag(IterateeTrace.Chunking());
    }
  }

  public boolean Iteratee.echo$chunking() {
    if (this._echo$tag != null) {
      return this.echo$tag().chunking();
    } else return false;
  }

  public IterateeTagData Iteratee.tag() {
    if (this._echo$tag == null) {
      this.echo$tag(IterateeTrace.ZeroIterateeTagData());
      return this._echo$tag;
    } else return this._echo$tag;
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

  private volatile IterateeInfo Iteratee._echo$info;

  public IterateeInfo Iteratee.echo$info() {
    if (_echo$info == null) return IterateeTrace.ZeroIterateeInfo();
    else return _echo$info;
  }

  public void Iteratee.echo$info(IterateeInfo info) {
    _echo$info = info;
  }

  public Info Iteratee.info() {
    return (Info) this._echo$info;
  }

  before(Iteratee iteratee):
    execution(play.api.libs.iteratee.Iteratee+.new(..)) &&
    this(iteratee)
  {
    ActionTracer tracer = ActionTracer.global();
    if (tracing(tracer)) {
      IterateeInfo info = IterateeTrace.newInfo();
      iteratee.echo$info(info);
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
      tracer.iteratee().folded(iteratee.echo$info());
      boolean chunking = iteratee.echo$chunking();
      return proceed(iteratee, new StepTracerHook(tracer, iteratee.echo$info(), chunking, folder), ctx);
    } else {
      return proceed(iteratee, folder, ctx);
    }
  }

}
