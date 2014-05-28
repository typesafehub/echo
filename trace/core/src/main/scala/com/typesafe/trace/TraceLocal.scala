/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */

package com.typesafe.trace

import com.typesafe.trace.util.Uuid
import scala.collection.mutable.{ ListBuffer, Stack }

object TraceLocal {
  /**
   * Current thread local information for creating trace trees.
   * A stack so that we can nest spans within a thread if needed.
   */
  val stack = new ThreadLocal[Stack[ActiveTrace]] {
    override def initialValue = Stack.empty[ActiveTrace]
  }
}

/**
 * Thread local and buffer storage for tracing.
 */
class TraceLocal(settings: Trace.Settings, sendOff: Batch ⇒ Unit) {
  import TraceLocal.stack

  val localLimit = settings.localLimit

  /**
   * Shared (across threads) buffer for trace events.
   * Flushes to trace event handlers when either size or time limit
   * is reached.
   */
  private val traceBuffer = new TraceBuffer(settings.sizeLimit, settings.timeLimit, sendOff)

  /**
   * Is there a current trace?
   */
  def within: Boolean = stack.get.nonEmpty

  /**
   * Sampling rate greater than 0 means we're tracing.
   */
  def sampled: Int = {
    val current = stack.get
    if (current.isEmpty) 0 else current.head.context.sampled
  }

  /**
   * Get the current trace local.
   */
  def current: Option[ActiveTrace] = stack.get.headOption

  /**
   * Start the scope of a new (possibly nested) span by pushing
   * a trace context on to the TraceLocal.stack.
   */
  def start(context: TraceContext): Unit = {
    println(">>> start(" + context + ")")
    stack.get.push(new ActiveTrace(context))
  }

  /**
   * If there is a currently active trace then store in the thread-local
   * stack until the current span has ended or the local limit is reached
   * before moving events to the shared trace buffer, otherwise add a single
   * node span to the trace buffer immediately.
   */
  def store(event: TraceEvent): Unit = {
    var local = true
    if (within) {
      val current = stack.get.head
      val events = current.events
      events += event
      if (events.size > localLimit) {
        local = false
        buffer(events.toSeq)
        current.events = ListBuffer.empty[TraceEvent]
      }
    } else {
      local = false
      buffer(Seq(event))
    }
    if (local) println("LOCAL++")
    else println("SHARED++")
  }

  /**
   * Add events to the shared (non-thread-local) trace buffer.
   */
  def buffer(events: Seq[TraceEvent]): Unit = {
    if (!events.isEmpty) {
      traceBuffer.add(TraceEvents(events))
    }
  }

  /**
   * End the current local span and move events to the trace buffer.
   */
  def end(): Unit = {
    if (stack.get.nonEmpty) {
      buffer(stack.get.pop.events.toSeq)
    }
  }

  /**
   * Flush the trace buffer (send off events).
   */
  def flush(): Unit = traceBuffer.send()
}
