/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */

package com.typesafe.trace

trait WithTracing {
  def enabled: Boolean
  def trace: Trace
  def sample: Int = if (trace.within) trace.sampled else 1

  def whenTracing[T](orElse: ⇒ T)(block: ⇒ T): T =
    if (enabled) block
    else orElse

  def whenTracingUnit(block: ⇒ Unit): Unit = whenTracing(()) { block; () }
  def whenTracingTC(block: ⇒ TraceContext): TraceContext =
    if (sample > 0)
      if (enabled) block
      else trace.continue
    else TraceContext.NoTrace

  def withTE(ev: Annotation): Unit = whenTracingUnit(trace.event(ev))
}
