/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */

package com.typesafe.trace

import akka.remote.RemoteLifeCycleEvent
import akka.remote.RemoteProtocol.{ MetadataEntryProtocol, RemoteMessageProtocol }
import com.google.protobuf.ByteString
import com.typesafe.trace.util.Uuid
import com.typesafe.trace.uuid.UUID

class RemoteTrace(trace: Trace) {

  val traceStatus = trace.settings.remoteLifeCycle
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

  def status(event: RemoteLifeCycleEvent): Unit = {
    if (traceStatus && trace.tracing) {
      trace.event(RemoteStatusTrace.remoteStatus(event))
    }
  }
}

object RemoteTrace {
  // short key names to reduce message size
  final val TraceMetadataEntryKey = "tc_t"
  final val ParentMetadataEntryKey = "tc_p"
  final val SampledMetadataEntryKey = "tc_s"

  def attachTraceContext(builder: RemoteMessageProtocol.Builder, context: TraceContext): Unit = {
    builder.addMetadata(
      MetadataEntryProtocol.newBuilder
        .setKey(TraceMetadataEntryKey)
        .setValue(uuidToByteString(context.trace))
        .build())

    builder.addMetadata(
      MetadataEntryProtocol.newBuilder
        .setKey(ParentMetadataEntryKey)
        .setValue(uuidToByteString(context.parent))
        .build())

    builder.addMetadata(
      MetadataEntryProtocol.newBuilder
        .setKey(SampledMetadataEntryKey)
        .setValue(intToByteString(context.sampled))
        .build())
  }

  def extractTraceContext(message: RemoteMessageProtocol): TraceContext = {
    var trace: UUID = null
    var parent: UUID = null
    var sampled = 1
    if (message.getMetadataCount != 0) {
      val iter = message.getMetadataList.iterator
      while (iter.hasNext) {
        val metadata = iter.next()
        try {
          if (metadata.getKey == TraceMetadataEntryKey) {
            trace = uuidFromByteString(metadata.getValue)
          }
          if (metadata.getKey == ParentMetadataEntryKey) {
            parent = uuidFromByteString(metadata.getValue)
          }
          if (metadata.getKey == SampledMetadataEntryKey) {
            sampled = intFromByteString(metadata.getValue)
          }
        } catch {
          case e: Exception ⇒ ()
        }
      }
    }
    if (trace == null || parent == null) TraceContext.ZeroTrace
    else TraceContext(trace, parent, sampled)
  }

  private def uuidToByteString(uuid: UUID): ByteString = {
    ByteString.copyFrom(TraceProtocol.ProtoUuid.newBuilder
      .setHigh(uuid.getTime)
      .setLow(uuid.getClockSeqAndNode)
      .build()
      .toByteArray)
  }

  private def uuidFromByteString(byteString: ByteString): UUID = {
    val uuid = TraceProtocol.ProtoUuid.newBuilder.mergeFrom(byteString).build()
    new UUID(uuid.getHigh, uuid.getLow)
  }

  private def intToByteString(value: Int): ByteString = {
    val bytes = Array.fill[Byte](4)(0)
    bytes(0) = (value >>> 24).asInstanceOf[Byte]
    bytes(1) = (value >>> 16).asInstanceOf[Byte]
    bytes(2) = (value >>> 8).asInstanceOf[Byte]
    bytes(3) = value.asInstanceOf[Byte]
    ByteString.copyFrom(bytes)
  }

  private def intFromByteString(byteString: ByteString): Int = {
    val bytes: Array[Byte] = byteString.toByteArray
    (0 until 4).foldLeft(0)((value, index) ⇒ value + ((bytes(index) & 0x000000FF) << ((4 - 1 - index) * 8)))
  }
}
