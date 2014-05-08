/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */

package com.typesafe.trace

import akka.remote.RemotingLifecycleEvent
import akka.util.ByteString
import com.typesafe.trace.TraceProtocol.{ ProtoTraceContext, ProtoUuid }
import com.typesafe.trace.util.ProtobufConverter
import com.typesafe.trace.util.Uuid
import com.typesafe.trace.uuid.UUID

class RemoteTrace(trace: Trace) {

  val traceLifecycle = trace.settings.remoteLifeCycle
  val remoteEvents = trace.settings.events.remoting

  def tracingRemote = remoteEvents && trace.tracing

  def sent(actor: ActorInfo, message: Any, messageSize: Int): TraceContext = {
    val sampled = trace.sampled
    if (sampled > 0)
      if (remoteEvents) trace.branch(RemoteMessageSent(actor, trace.formatShortMessage(message), messageSize), sampled)
      else trace.continue
    else if (trace.within && sampled == 0) TraceContext.NoTrace
    else TraceContext.EmptyTrace
  }

  def received(actor: ActorInfo, message: Any, messageSize: Int): Unit = {
    if (tracingRemote) {
      trace.event(RemoteMessageReceived(actor, trace.formatShortMessage(message), messageSize))
    }
  }

  def completed(actor: ActorInfo, message: Any): Unit = {
    if (tracingRemote) {
      trace.event(RemoteMessageCompleted(actor, trace.formatShortMessage(message)))
    }
  }

  def lifecycle(event: RemotingLifecycleEvent): Unit = {
    if (traceLifecycle && trace.tracing) {
      trace.event(RemotingLifecycleTrace.remotingLifecycle(event))
    }
  }
}

object RemoteTrace {

  def attachTraceContext(bytes: ByteString, context: TraceContext): ByteString = {
    bytes ++ ByteString(ProtobufConverter.toProtoTraceContext(context).toByteArray)
  }

  def extractTraceContext(bytes: ByteString): (ByteString, TraceContext) = {
    try {
      val protoContext = ProtoTraceContext.parseFrom(bytes.toArray)
      val remaining = ByteString(protoContext.getUnknownFields.toByteArray)
      (remaining, ProtobufConverter.fromProtoTraceContext(protoContext))
    } catch {
      case _: Exception ⇒ (bytes, TraceContext.ZeroTrace)
    }
  }
}
