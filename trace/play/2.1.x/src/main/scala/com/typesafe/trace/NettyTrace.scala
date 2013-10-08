/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */

package com.typesafe.trace

import com.typesafe.trace.util.Uuid
import org.jboss.netty.handler.codec.http.{ HttpChunk, HttpRequest, HttpHeaders, HttpMessage }
import org.jboss.netty.channel.Channel;

private[trace] object NettyTrace {
  case class DeferredData(channel: Channel,
                          readBytes: Int)

  def deferredData(channel: Channel, readBytes: Int): DeferredData =
    DeferredData(channel, readBytes)
}

/**
 * Tracing events of Actions.
 */
private[trace] class NettyTrace(val trace: Trace) extends WithTracing {
  import NettyTrace._

  lazy val nettyEvents = trace.settings.events.netty

  def enabled: Boolean = nettyEvents && trace.tracing

  def traceUriContext(uri: String): TraceContext = {
    val curi: String = uri.split("\\?")(0).trim.reverse.dropWhile(_ == '/').reverse
    if (trace.playTraceableSample(curi) <= 0) TraceContext.NoTrace
    else null
  }

  def httpReceivedStart(): TraceContext =
    whenTracingTC(trace.branch(NettyHttpReceivedStart, sample))

  def httpReceivedEnd(): Unit =
    withTE(NettyHttpReceivedEnd)

  def playReceivedStart(): Unit =
    withTE(NettyPlayReceivedStart)

  def playReceivedEnd(): Unit =
    withTE(NettyPlayReceivedEnd)

  def responseHeader(size: Int): Unit =
    withTE(NettyResponseHeader(size))

  def responseBody(size: Int): Unit =
    withTE(NettyResponseBody(size))

  def writeChunk(overhead: Int, size: Int): Unit =
    withTE(NettyWriteChunk(overhead, size))

  def readBytes(size: Int): Unit =
    withTE(NettyReadBytes(size))
}
