/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */

package com.typesafe.atmos.trace

import akka.actor.{ ActorPath, ActorRef, Props }
import akka.atmos.trace.MessageTrace
import akka.dispatch.NullMessage
import akka.remote.RemoteActorRef

object TracedActorInfo {
  val none = ActorInfo("untraced", None, false, false, Set.empty)

  def apply(actorRef: ActorRef): ActorInfo = TraceInfo(actorRef) match {
    case info: ActorInfo ⇒ info
    case _               ⇒ none
  }
}

/**
 * Tracing actor events. Internal API.
 */
class ActorTrace(trace: Trace) {

  val actorEvents = trace.settings.events.actors
  val tempActorEvents = actorEvents && trace.settings.events.tempActors

  def tracingActors = actorEvents && trace.tracing
  def tracingTempActors = tempActorEvents && trace.tracing

  // -----------------------------------------------
  // Message events
  // -----------------------------------------------

  // seperate for accessing akka internals
  val message = new MessageTrace(trace)

  // -----------------------------------------------
  // Actor information
  // -----------------------------------------------

  def identifier(path: ActorPath): String = {
    path.elements.mkString("/", "/", "")
  }

  def traceable(identifier: String): Boolean = {
    trace.actorTraceable(identifier)
  }

  def info(path: ActorPath, dispatcher: String, remote: Boolean): ActorInfo = {
    ActorInfo(path.toString, Option(dispatcher), remote, false, trace.actorTags(identifier(path)))
  }

  // -----------------------------------------------
  // System events
  // -----------------------------------------------

  def systemStarted(startTime: Long) {
    trace.event(SystemStarted(startTime))
  }

  def systemShutdown(shutdownTime: Long) {
    trace.event(SystemShutdown(shutdownTime))
  }

  def requestedTopLevelActor(guardianId: String, guardian: ActorInfo, name: String): TraceContext = {
    val sampled = if (trace.within) trace.sampled else 1
    if (sampled > 0)
      if (actorEvents) trace.branch(TopLevelActorRequested(guardian, name), sampled)
      else trace.continue
    else TraceContext.NoTrace
  }

  def createdTopLevelActor(actor: ActorInfo): Unit = {
    if (tracingActors) {
      trace.event(TopLevelActorCreated(actor))
    }
  }

  // -----------------------------------------------
  // Actor events
  // -----------------------------------------------

  def requested(actor: ActorInfo, child: ActorInfo): TraceContext = {
    val sampled = trace.sampled
    if (sampled > 0)
      if (actorEvents) trace.branch(ActorRequested(actor, child), sampled)
      else trace.continue
    else if (trace.within && sampled == 0) TraceContext.NoTrace
    else TraceContext.EmptyTrace
  }

  def created(actor: ActorInfo): Unit = {
    if (tracingActors) {
      trace.event(ActorCreated(actor))
    }
  }

  def told(identifier: String, actor: ActorInfo, message: Any, sender: ActorInfo): TraceContext = {
    val sampled = if (message != NullMessage) trace.actorSampleBranch(identifier) else 0
    if (sampled > 0)
      if (actorEvents) trace.branch(ActorTold(actor, trace.formatLongMessage(message), Option(sender)), sampled)
      else trace.continue
    else TraceContext.NoTrace
  }

  def asked(identifier: String, actor: ActorInfo, message: Any): TraceContext = {
    val sampled = trace.actorSampleBranch(identifier)
    if (sampled > 0)
      if (actorEvents) trace.branch(ActorAsked(actor, trace.formatLongMessage(message)), sampled)
      else trace.continue
    else TraceContext.NoTrace
  }

  def failed(actor: ActorInfo, reason: Throwable, supervisor: ActorInfo): TraceContext = {
    val sampled = trace.sampled
    if (sampled > 0)
      if (actorEvents) trace.branch(ActorFailed(actor, trace.formatLongException(reason), supervisor), sampled)
      else trace.continue
    else if (trace.within && sampled == 0) TraceContext.NoTrace
    else TraceContext.EmptyTrace
  }

  // -----------------------------------------------
  // Temp actor events
  // -----------------------------------------------

  def tempCreated(actor: ActorInfo): Unit = {
    if (tracingTempActors) {
      trace.event(TempActorCreated(actor))
    }
  }

  def tempTold(identifier: String, actor: ActorInfo, message: Any, sender: ActorInfo): TraceContext = {
    val sampled = if (message != NullMessage) trace.actorSampleBranch(identifier) else 0
    if (sampled > 0)
      if (tempActorEvents) trace.branch(ActorTold(actor, trace.formatLongMessage(message), Option(sender)), sampled)
      else trace.continue
    else TraceContext.NoTrace
  }

  def tempStopped(actor: ActorInfo): Unit = {
    if (tracingTempActors) {
      trace.event(TempActorStopped(actor))
    }
  }
}
