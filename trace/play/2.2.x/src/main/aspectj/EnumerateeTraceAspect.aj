/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */

package com.typesafe.atmos.trace;

import java.lang.System;
import play.api.libs.iteratee.Iteratee;
import play.api.libs.iteratee.Enumeratee;

privileged aspect EnumerateeTraceAspect {

  // add trace metadata to Enumeratee

  declare parents: Enumeratee implements EnumerateeTag;

  private volatile EnumerateeTagData Enumeratee._atmos$tag;

  public EnumerateeTagData Enumeratee.atmos$tag() {
    return _atmos$tag;
  }

  public void Enumeratee.atmos$tag(EnumerateeTagData tag) {
    _atmos$tag = tag;
  }

  public void Enumeratee.atmos$setChunking() {
    if (this._atmos$tag == null) {
      this.atmos$tag(EnumerateeTrace.Chunking());
    }
  }

  public boolean Enumeratee.atmos$chunking() {
    if (this._atmos$tag != null) {
      return this.atmos$tag().chunking();
    } else return false;
  }

  public EnumerateeTagData Enumeratee.tag() {
    return this._atmos$tag;
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

  // def applyOn[A](inner: Iteratee[To, A]): Iteratee[From, Iteratee[To, A]]
  Object around(Enumeratee enumeratee, Iteratee inner):
    execution(* play.api.libs.iteratee.Enumeratee+.applyOn(Iteratee)) &&
    this(enumeratee) &&
    args(inner)
  {
    ActionTracer tracer = ActionTracer.global();
    if (tracing(tracer)) {
      if (enumeratee.atmos$chunking()) inner.atmos$setChunking();
      return proceed(enumeratee, inner);
    } else {
      return proceed(enumeratee, inner);
    }
  }

}
