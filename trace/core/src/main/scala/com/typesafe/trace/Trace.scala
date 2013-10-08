/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */

package com.typesafe.trace

import com.typesafe.config.{ Config, ConfigFactory }
import java.lang.management.ManagementFactory
import scala.util.control.Exception._

/**
 * Tracing. Internal API.
 */
object Trace {
  val DispatcherId = "atmos-trace-dispatcher"

  /**
   * Wrapper class for all trace settings.
   * TODO: general trace settings should be here, scala and akka specific settings elsewhere
   */
  class Settings(val systemName: String, systemConfig: Config, classLoader: ClassLoader) {

    val config: Config = systemConfig.withFallback(ConfigFactory.defaultReference(classLoader))
    config.checkValid(ConfigFactory.defaultReference, "atmos")

    val enabled = config.getBoolean("atmos.trace.enabled")

    val node = TraceEvent.normalizeNode({
      def syntheticNodeName(processId: String) = systemName + "-" + processId + "@" + TraceEvent.HostName

      val key = "atmos.trace.node"
      allCatch.opt {
        config.getString(key) match {
          case "" ⇒
            val fullName = ManagementFactory.getRuntimeMXBean.getName
            syntheticNodeName(fullName.substring(0, fullName.indexOf("@")))
          case n ⇒ n
        }
      }.getOrElse(syntheticNodeName(System.currentTimeMillis.toString))
    })

    val sendPort = config.getInt("atmos.trace.send.port")
    val sendCapacity = config.getInt("atmos.trace.send.capacity")
    val sendRetry = config.getBoolean("atmos.trace.send.retry")
    val sendDaemonic = config.getBoolean("atmos.trace.send.daemonic") || config.getBoolean("akka.daemonic")
    val sendGlobalDaemonic = config.getBoolean("atmos.trace.send.global.daemonic")
    val sendWarn = config.getBoolean("atmos.trace.send.warn")

    val zeroContextFutures = config.getBoolean("atmos.trace.futures")
    val zeroContextIteratees = config.getBoolean("atmos.trace.iteratees")

    val events = new TraceEvent.Settings(config.getConfig("atmos.trace.events"))

    val localLimit = config.getInt("atmos.trace.buffer.local-limit")
    val sizeLimit = config.getInt("atmos.trace.buffer.size-limit")
    val timeLimit = config.getMilliseconds("atmos.trace.buffer.time-limit").intValue

    val maxLengthShortMessage = config.getInt("atmos.trace.max-length-short-message")
    val maxLengthLongMessage = config.getInt("atmos.trace.max-length-long-message")
    val maxStackTraceSize = config.getInt("atmos.trace.max-stack-trace-size")

    val remoteLifeCycle = config.getBoolean("atmos.trace.remote-life-cycle")

    val useDispatcherMonitor = config.getBoolean("atmos.trace.use-dispatcher-monitor")
    val dispatcherPollInterval = config.getMilliseconds("atmos.trace.dispatcher-poll-interval")

    val useSystemMetricsMonitor = config.getBoolean("atmos.trace.use-system-metrics-monitor")
    val systemMetricsPollInterval = config.getMilliseconds("atmos.trace.system-metrics-poll-interval")
    val deadlockWatchFrequency = config.getMilliseconds("atmos.trace.deadlock-watch-frequency")

    val shutdownTimeout = config.getMilliseconds("atmos.trace.shutdown-timeout")

    override def toString: String = config.root.render
  }
}

/**
 * Actual tracing used by Tracer here. Internal API.
 */
class Trace(val settings: Trace.Settings, global: Boolean = false) {

  val daemonic = if (global) (settings.sendGlobalDaemonic || settings.sendDaemonic) else settings.sendDaemonic

  val sender = TraceSender(settings.sendPort, settings.sendCapacity, settings.sendRetry, daemonic, settings.sendWarn)

  val local = new TraceLocal(settings, sender.send)

  val actorTraceability = Traceable.actorTraceable(settings)

  val playTraceability = Traceable.playTraceable(settings)

  /**
   * Is this id traceable?
   */
  def actorTraceable(id: String): Boolean = actorTraceability.traceable(id)

  /**
   * Is this Play path traceable?
   */
  def playTraceable(path: String): Boolean = playTraceability.traceable(path)

  /**
   * Should we trace this branch identifier?
   * NB: updates a sampling counter as a side-effect.
   * Returns the sampling rate (0 is no tracing).
   */
  def actorSampleBranch(id: String): Int = {
    if (within) {
      val current = sampled
      if (current < 0) actorSample(id) else current
    } else {
      actorSample(id)
    }
  }

  /**
   * Should we trace this branch identifier?
   * NB: updates a sampling counter as a side-effect.
   * Returns the sampling rate (0 is no tracing).
   */
  def playSampleBranch(path: String): Int = {
    if (within) {
      val current = sampled
      if (current < 0) playSample(path) else current
    } else {
      playSample(path)
    }
  }

  /**
   * Returns a sample rate > 0 if we should sample this trace.
   * NB: updates a sampling counter as a side-effect.
   */
  def actorSample(id: String): Int = actorTraceability.sample(id)

  /**
   * Returns a sample rate > 0 if we should sample this trace.
   * NB: updates a sampling counter as a side-effect.
   */
  def playSample(id: String): Int = playTraceability.sample(id)

  /**
   * Returns a sample rate > 0 if we should sample this trace.
   * NB: updates a sampling counter as a side-effect.
   */
  def playTraceableSample(id: String): Int =
    if (playTraceable(id)) playSample(id) else 0

  /**
   * Returns the set of tags configured for this id.
   */
  def actorTags(id: String): Set[String] = actorTraceability.tags(id)

  /**
   * Is there a current trace?
   */
  def within: Boolean = local.within

  /**
   * Current sampled rate.
   */
  def sampled: Int = local.sampled

  /**
   * Are we tracing? Defaults to true outside of traces.
   */
  def tracing: Boolean = {
    if (within) sampled > 0 else true
  }

  /**
   * Are we actively tracing? Defaults to false outside of traces.
   */
  def active: Boolean = {
    if (within) sampled > 0 else false
  }

  /**
   * Branch the trace tree.
   *
   * Returns a TraceContext which may be used to start a nested span on
   * the same thread with ``local.start``, or stored for transfer to other
   * threads or nodes using ``local.store`` or other means.
   */
  def branch(annotation: Annotation, sampled: Int): TraceContext = {
    val event = newEvent(annotation, sampled)
    local.store(event)
    TraceContext(event.trace, event.id, sampled)
  }

  /**
   * Get the current TraceContext for continuing the trace elsewhere,
   * but without creating a branching event.
   */
  def continue(): TraceContext = {
    local.current match {
      case Some(activeTrace) ⇒ activeTrace.context
      case None              ⇒ TraceContext.ZeroTrace
    }
  }

  def cont(): TraceContext = continue()

  /**
   * Record a new trace event wrapping an annotation.
   */
  def event(annotation: Annotation): Unit = {
    local.store(newEvent(annotation))
  }

  /**
   * Create a new trace event.
   */
  def newEvent(annotation: Annotation, sampled: Int = 1): TraceEvent = {
    TraceEvent(annotation, settings.systemName, local.current, sampled, settings.node)
  }

  // -----------------------------------------------
  // Formatting messages
  // -----------------------------------------------

  def formatShortMessage(msg: Any): String = msg match {
    case m: TraceMessage                              ⇒ m.toShortTraceMessage
    case other if settings.maxLengthShortMessage == 0 ⇒ ""
    case null                                         ⇒ "null"
    case other ⇒
      val str = msg.toString
      if (str.length > settings.maxLengthShortMessage) str.take(settings.maxLengthShortMessage) else str
  }

  def formatLongMessage(msg: Any): String = msg match {
    case m: TraceMessage                             ⇒ m.toLongTraceMessage
    case other if settings.maxLengthLongMessage == 0 ⇒ ""
    case null                                        ⇒ "null"
    case other ⇒
      val str = msg.toString
      if (str.length > settings.maxLengthLongMessage) str.take(settings.maxLengthLongMessage) else str
  }

  def formatShortException(exception: Throwable): String = {
    "%s: %s".format(exception.getClass.getName, exception.getMessage)
  }

  def formatLongException(exception: Throwable): String = {
    "%s: %s\n%s".format(exception.getClass.getName, exception.getMessage, formatStackTrace(exception))
  }

  def formatStackTrace(exception: Throwable) = {
    val stackTrace = exception.getStackTrace
    val sb = new StringBuilder
    val length = math.min(stackTrace.length, 20)
    for (i ← 0 until length)
      sb.append("\tat %s\n" format stackTrace(i))
    sb.toString
  }
}
