/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */

package com.typesafe.trace

import com.typesafe.trace.util.Uuid
import com.typesafe.trace.uuid.UUID
import scala.collection.mutable.ListBuffer

/**
 * Contains contextual information for tracing.
 */
case class TraceContext(
  trace: UUID = Uuid(),
  parent: UUID = Uuid(),
  sampled: Int = 1) {

  def isTracing: Boolean = sampled > 0

  def isEmpty: Boolean = sampled < 0
}

object TraceContext {
  /**
   * Base context.
   */
  val ZeroTrace = TraceContext(Uuid.zero, Uuid.zero, 1)

  /**
   * Don't trace context.
   */
  val NoTrace = TraceContext(Uuid.zero, Uuid.zero, 0)

  /**
   * Empty non-traced context. Some branches may begin tracing.
   */
  val EmptyTrace = TraceContext(Uuid.zero, Uuid.zero, -1)
}

/**
 * Contains information for the currently active trace.
 */
class ActiveTrace(val context: TraceContext) {
  val local = if (context.isEmpty) Uuid.zero else Uuid()
  var events = ListBuffer.empty[TraceEvent]
}
