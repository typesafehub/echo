/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */

package com.typesafe.trace;

import com.typesafe.trace.IterateeTrace.StepTracerHook;
import java.lang.System;
import play.api.libs.iteratee.Iteratee;
import scala.Function1;

privileged aspect IterateeTraceAspect {

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

  Object around(Iteratee iteratee, Function1 folder):
    execution(* play.api.libs.iteratee.Iteratee+.fold(Function1)) &&
    this(iteratee) &&
    args(folder)
  {
    ActionTracer tracer = ActionTracer.global();
    if (tracing(tracer)) {
      tracer.iteratee().folded(iteratee.echo$info());
      return proceed(iteratee, new StepTracerHook(tracer, iteratee.echo$info(), folder));
    } else {
      return proceed(iteratee, folder);
    }
  }

}
