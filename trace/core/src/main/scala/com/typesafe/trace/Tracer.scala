/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */

package com.typesafe.trace

/**
 * Interface to make it possible to get attached Tracer from Scala.
 */
trait WithTracer {
  def tracer: Tracer
}

/**
 * User API for tracing.
 */
object Tracer {
  /**
   * Get the attached tracer for some object, for example, an ActorSystem.
   */
  def apply(traced: Any): Tracer = traced match {
    case attached: WithTracer ⇒ attached.tracer
    case _                    ⇒ EmptyTracer
  }
}

/**
 * User API for tracing.
 */
trait Tracer {
  /**
   * Record a key-value Marker.
   */
  def record(name: String, data: String): Unit

  /**
   * Run the block within MarkStarted and MarkEnded annotations,
   * i.e. user defined marker span.
   *
   */
  def mark[B](name: String)(body: ⇒ B): B

  /**
   * Record a MarkerStarted annotation, which is paired with MarkerEnded
   * with same name to create a user defined marker span. A user defined span may
   * start and end in any location in the message trace, i.e. it may span over several
   * actors.
   */
  def markStart(name: String): Unit

  /**
   * Record a MarkerEnded annotation, which is paired with MarkerStarted
   * with same name to create a user defined marker span. A user defined span may
   * start and end in any location in the message trace, i.e. it may span over several
   * actors.
   */
  def markEnd(name: String): Unit

  /**
   * Grouping together events as a single trace tree node.
   */
  def group[B](name: String)(body: ⇒ B): B

  /**
   * Start a new group, grouping together events as a single trace tree node.
   * startGroup and endGroup must be used together running with the same thread.
   */
  def startGroup(name: String): Unit

  /**
   * Start a new group, grouping together events as a single trace tree node.
   * startGroup and endGroup must be used together running with the same thread.
   */
  def endGroup(name: String): Unit
}

/**
 * Default implementation of User API.
 */
abstract class DefaultTracer extends Tracer {
  def trace: Trace

  // -----------------------------------------------
  // User API implementation
  // -----------------------------------------------

  def record(name: String, data: String): Unit = {
    if (trace.tracing) {
      trace.event(Marker(name, data))
    }
  }

  def mark[B](name: String)(body: ⇒ B): B = {
    markStart(name)
    try {
      body
    } finally {
      markEnd(name)
    }
  }

  def markStart(name: String): Unit = {
    if (trace.tracing) {
      trace.event(MarkerStarted(name))
    }
  }

  def markEnd(name: String): Unit = {
    if (trace.tracing) {
      trace.event(MarkerEnded(name))
    }
  }

  def group[B](name: String)(body: ⇒ B): B = {
    startGroup(name)
    try {
      body
    } finally {
      endGroup(name)
    }
  }

  def startGroup(name: String): Unit = {
    val sampled = trace.actorSampleBranch(name)
    val context = if (sampled > 0) trace.branch(GroupStarted(name), sampled) else TraceContext.NoTrace
    trace.local.start(context)
  }

  def endGroup(name: String): Unit = {
    if (trace.tracing) {
      trace.event(GroupEnded(name))
    }
    trace.local.end()
  }
}

/**
 * Tracer that does nothing. Used when no tracer is attached.
 */
object EmptyTracer extends Tracer {
  def record(name: String, data: String): Unit = ()
  def mark[B](name: String)(body: ⇒ B): B = body
  def markStart(name: String): Unit = ()
  def markEnd(name: String): Unit = ()
  def group[B](name: String)(body: ⇒ B): B = body
  def startGroup(name: String): Unit = ()
  def endGroup(name: String): Unit = ()
}
