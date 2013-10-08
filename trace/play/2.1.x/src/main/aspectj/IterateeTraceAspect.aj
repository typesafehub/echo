/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */

package com.typesafe.atmos.trace;

import com.typesafe.atmos.trace.IterateeTrace.StepTracerHook;
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

  Object around(Iteratee iteratee, Function1 folder):
    execution(* play.api.libs.iteratee.Iteratee+.fold(Function1)) &&
    this(iteratee) &&
    args(folder)
  {
    ActionTracer tracer = ActionTracer.global();
    if (tracing(tracer)) {
      tracer.iteratee().folded(iteratee.atmos$info());
      return proceed(iteratee, new StepTracerHook(tracer, iteratee.atmos$info(), folder));
    } else {
      return proceed(iteratee, folder);
    }
  }

}
