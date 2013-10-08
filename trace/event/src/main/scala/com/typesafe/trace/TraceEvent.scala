/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */

package com.typesafe.trace

import com.typesafe.config.Config
import com.typesafe.trace.util.Uuid
import com.typesafe.trace.uuid.UUID
import java.net.{ InetAddress, UnknownHostException }

/**
 * A batch of TraceEvents.
 */
case class Batch(payload: Seq[TraceEvents])

/**
 * Collection of trace events.
 */
case class TraceEvents(events: Seq[TraceEvent])

/**
 * Trace event.
 */
case class TraceEvent(
  annotation: Annotation,
  id: UUID,
  trace: UUID,
  local: UUID,
  parent: UUID,
  sampled: Int,
  node: String,
  host: String,
  actorSystem: String,
  timestamp: Long,
  nanoTime: Long)

/**
 * Creating trace events.
 */
object TraceEvent {
  /**
   * The hostname for this node.
   */
  val HostName: String = try {
    InetAddress.getLocalHost.getCanonicalHostName
  } catch {
    case e: UnknownHostException ⇒ "unknown"
  }

  /**
   * Default logical node name.
   */
  val DefaultNode = "default@" + HostName

  /**
   * Accepted node characters. For regular expressions.
   */
  val NodeChars = """\w\-\.\:@"""

  /**
   * Normalize node names to accepted characters only.
   */
  def normalizeNode(node: String) = {
    node.replaceAll("[^" + NodeChars + """\s]+""", "").trim.replaceAll("""\s+""", "-")
  }

  /**
   * Trace event settings for enabling or disabling specific events.
   */
  class Settings(config: Config) {
    val futures = config.getBoolean("futures")
    val actors = config.getBoolean("actors")
    val systemMessages = config.getBoolean("system-messages")
    val tempActors = config.getBoolean("temp-actors")
    val remoting = config.getBoolean("remoting")
    val scheduler = config.getBoolean("scheduler")
    val runnables = config.getBoolean("runnables")
    val eventStream = config.getBoolean("event-stream")
    val netty = config.getBoolean("netty")
    val iteratees = config.getBoolean("iteratees")
    val actions = config.getBoolean("actions")
  }

  def apply(
    annotation: Annotation,
    actorSystem: String,
    currentTrace: Option[ActiveTrace] = None,
    sampled: Int = 1,
    node: String = DefaultNode): TraceEvent = currentTrace match {
    case Some(activeTrace) ⇒ {
      val context = activeTrace.context
      val local = activeTrace.local
      val events = activeTrace.events
      TraceEvent(
        annotation = annotation,
        id = Uuid(),
        trace = if (context.isEmpty) Uuid() else context.trace,
        local = local,
        parent = if (events.isEmpty) context.parent else events.last.id,
        sampled = if (context.isEmpty) sampled else context.sampled,
        node = node,
        host = HostName,
        actorSystem = actorSystem,
        timestamp = System.currentTimeMillis,
        nanoTime = System.nanoTime)
    }
    case None ⇒ {
      TraceEvent(
        annotation = annotation,
        id = Uuid(),
        trace = Uuid(),
        local = Uuid.zero,
        parent = Uuid.zero,
        sampled = sampled,
        node = node,
        host = HostName,
        actorSystem = actorSystem,
        timestamp = System.currentTimeMillis,
        nanoTime = System.nanoTime)
    }
  }
}
