/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */

package com.typesafe.atmos.trace

import com.typesafe.config.{ Config, ConfigFactory }
import java.util.concurrent.{ CountDownLatch, TimeoutException, TimeUnit }
import scala.concurrent.ExecutionContext

object ExecutionContextTracer {
  val global: ExecutionContextTracer = create()

  /**
   * Create a new ExecutionContextTracer. First checks config for whether tracing is enabled.
   * If tracing is not enabled then an empty DisabledExecutionContextTracer is returned.
   */
  def create(): ExecutionContextTracer = {
    val settings = new Trace.Settings("ExecutionContext", ConfigFactory.load, Thread.currentThread.getContextClassLoader)
    if (settings.enabled) new DefaultExecutionContextTracer(settings)
    else new DisabledExecutionContextTracer(settings)
  }
}

/**
 * Interface for ExecutionContext tracer.
 */
abstract class ExecutionContextTracer extends DefaultTracer {
  def enabled: Boolean
  def trace: Trace
  def executionContext: ExecutionContextTrace
  def future: FutureTrace
}

/**
 * Tracer implementation. Internal API.
 */
class DefaultExecutionContextTracer(val settings: Trace.Settings) extends ExecutionContextTracer {
  val trace = new Trace(settings, global = true)

  val executionContext = new ExecutionContextTrace(trace)

  val future = new FutureTrace(trace)

  def enabled = settings.enabled

  override def toString = "ExecutionContextTracer(%s)" format settings.systemName
}

/**
 * Disabled tracer. Enabled needs to be checked to avoid null pointer exceptions.
 */
class DisabledExecutionContextTracer(val settings: Trace.Settings) extends ExecutionContextTracer {
  val enabled = false
  val trace: Trace = null
  val executionContext: ExecutionContextTrace = null
  val future: FutureTrace = null
  override def record(name: String, data: String): Unit = ()
  override def mark[B](name: String)(body: ⇒ B): B = body
  override def markStart(name: String): Unit = ()
  override def markEnd(name: String): Unit = ()
  override def group[B](name: String)(body: ⇒ B): B = body
  override def startGroup(name: String): Unit = ()
  override def endGroup(name: String): Unit = ()
  override def toString = "ExecutionContextTracer(%s)" format settings.systemName
}
