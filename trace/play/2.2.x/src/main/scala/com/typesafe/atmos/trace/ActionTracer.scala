/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */

package com.typesafe.atmos.trace

import com.typesafe.config.{ Config, ConfigFactory }
import play.api.mvc.{ Result, PlainResult, AsyncResult, Request }
import play.core.Router.HandlerDef
import scala.concurrent.Future

private[atmos] object ActionTracer {
  val global: ActionTracer = create()

  /**
   * Create a new ActionTracer. First checks config for whether tracing is enabled.
   * If tracing is not enabled then an empty DisabledActionTracer is returned.
   */
  def create(): ActionTracer = {
    val settings = new Trace.Settings("Action", ConfigFactory.load, Thread.currentThread.getContextClassLoader)
    if (settings.enabled) new DefaultActionTracer(settings)
    else new DisabledActionTracer(settings)
  }
}

/**
 * Interface for Action tracer.
 */
private[atmos] abstract class ActionTracer extends DefaultTracer {
  def enabled: Boolean
  def trace: Trace
  def action: ActionTrace
  def iteratee: IterateeTrace
  def netty: NettyTrace
}

/**
 * Tracer implementation. Internal API.
 */
private[atmos] class DefaultActionTracer(settings: Trace.Settings) extends ActionTracer {
  val trace = new Trace(settings)

  val action = new ActionTrace(trace)
  val iteratee = new IterateeTrace(trace)
  val netty = new NettyTrace(trace)

  def enabled = trace.settings.enabled

  override def toString = "ActionTracer(%s)" format trace.settings.systemName
}

private[atmos] class DisabledActionTracer(settings: Trace.Settings) extends ActionTracer {
  val trace = null
  val action = null
  val iteratee = null
  val netty = null

  def enabled = false

  override def record(name: String, data: String): Unit = ()
  override def mark[B](name: String)(body: ⇒ B): B = body
  override def markStart(name: String): Unit = ()
  override def markEnd(name: String): Unit = ()
  override def group[B](name: String)(body: ⇒ B): B = body
  override def startGroup(name: String): Unit = ()
  override def endGroup(name: String): Unit = ()

  override def toString = "ActionTracer(%s)" format settings.systemName
}
