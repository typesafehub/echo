/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */

package com.typesafe.trace;

import java.lang.System;
import play.api.libs.iteratee.Iteratee;
import play.api.libs.iteratee.Enumeratee;

privileged aspect EnumerateeTraceAspect {

  // add trace metadata to Enumeratee

  declare parents: Enumeratee implements EnumerateeTag;

  private volatile EnumerateeTagData Enumeratee._echo$tag;

  public EnumerateeTagData Enumeratee.echo$tag() {
    return _echo$tag;
  }

  public void Enumeratee.echo$tag(EnumerateeTagData tag) {
    _echo$tag = tag;
  }

  public void Enumeratee.echo$setChunking() {
    if (this._echo$tag == null) {
      this.echo$tag(EnumerateeTrace.Chunking());
    }
  }

  public boolean Enumeratee.echo$chunking() {
    if (this._echo$tag != null) {
      return this.echo$tag().chunking();
    } else return false;
  }

  public EnumerateeTagData Enumeratee.tag() {
    return this._echo$tag;
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
      if (enumeratee.echo$chunking()) {
        System.out.println("-- play.api.libs.iteratee.Enumeratee+.applyOn: chunking");
        inner.echo$setChunking();
        System.out.println("-- set on inner");
      }
      return proceed(enumeratee, inner);
    } else {
      return proceed(enumeratee, inner);
    }
  }

}
