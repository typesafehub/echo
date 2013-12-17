/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */

package com.typesafe.trace

import com.typesafe.trace.util.Uuid
import org.jboss.netty.handler.codec.http.{ HttpChunk, HttpRequest, HttpHeaders, HttpMessage, HttpResponse }
import org.jboss.netty.channel.{ ChannelFuture, ChannelFutureListener, Channel }
import java.util.concurrent.TimeUnit
import java.util.concurrent.ConcurrentHashMap

private[trace] object NettyTrace {
  case class DeferredData(channel: Channel,
                          readBytes: Int)

  def deferredData(channel: Channel, readBytes: Int): DeferredData =
    DeferredData(channel, readBytes)

  // Fix for #1229
  // This line:
  // https://github.com/playframework/playframework/blob/2.2.x/framework/src/play/src/main/scala/play/core/server/netty/NettyPromise.scala#L17
  // Creates a future that can be completed at any time including *before it is scheduled/executed*!
  // As a result we need to check to see if we are within a span, if not then we need to "connect"
  // to the trace-tree associated with the current Play request.
  class ProxyChannelFutureListener(underlying: ChannelFutureListener, context: TraceContext, trace: Trace) extends ChannelFutureListener {
    def operationComplete(future: ChannelFuture): Unit = {
      if (trace.within)
        underlying.operationComplete(future)
      else {
        trace.local.start(context)
        underlying.operationComplete(future)
        trace.local.end()
      }
    }
  }

  class ProxyChannelFuture(underlying: ChannelFuture, context: TraceContext, trace: Trace) extends ChannelFuture {
    private final val listeners: ConcurrentHashMap[ChannelFutureListener, ChannelFutureListener] = new ConcurrentHashMap[ChannelFutureListener, ChannelFutureListener]
    def getChannel: Channel = underlying.getChannel
    def isDone: Boolean = underlying.isDone
    def isCancelled: Boolean = underlying.isCancelled
    def isSuccess: Boolean = underlying.isSuccess
    def getCause: Throwable = underlying.getCause
    def cancel: Boolean = underlying.cancel
    def setSuccess: Boolean = underlying.setSuccess
    def setFailure(cause: Throwable): Boolean = underlying.setFailure(cause)
    def setProgress(amount: Long, current: Long, total: Long): Boolean = underlying.setProgress(amount, current, total)
    def addListener(listener: ChannelFutureListener): Unit = {
      val proxy = new ProxyChannelFutureListener(listener, context, trace)
      val previous = listeners.putIfAbsent(listener, proxy)
      if (previous != null) underlying.removeListener(previous)
      underlying.addListener(proxy)
    }
    def removeListener(listener: ChannelFutureListener): Unit = {
      val proxy = listeners.remove(listener)
      if (proxy != null) underlying.removeListener(proxy)
    }
    def rethrowIfFailed: ChannelFuture = underlying.rethrowIfFailed
    def sync: ChannelFuture = underlying.sync
    def syncUninterruptibly: ChannelFuture = underlying.syncUninterruptibly
    def await: ChannelFuture = underlying.await
    def awaitUninterruptibly: ChannelFuture = underlying.awaitUninterruptibly
    def await(timeout: Long, unit: TimeUnit): Boolean = underlying.await(timeout, unit)
    def await(timeoutMillis: Long): Boolean = underlying.await(timeoutMillis)
    def awaitUninterruptibly(timeout: Long, unit: TimeUnit): Boolean = underlying.awaitUninterruptibly(timeout, unit)
    def awaitUninterruptibly(timeoutMillis: Long): Boolean = underlying.awaitUninterruptibly(timeoutMillis)
  }

  def proxyChannelFuture(underlying: ChannelFuture, trace: Trace): ChannelFuture =
    new ProxyChannelFuture(underlying, trace.continue, trace)

  final private val SEPERATOR_LENGTH: Int = 2
  final private val NEWLINE_LENGTH: Int = 2

  // This is not ideal.  I'm reconstructing the request and response header blocks.
  final def requestHeaderLength(message: HttpRequest): Int = {
    message.getMethod.getName.getBytes("UTF-8").length + 1 + message.getUri.getBytes("UTF-8").length +
      message.getProtocolVersion.toString.getBytes("UTF-8").length + NEWLINE_LENGTH + httpHeadersLength(message)
  }
  final def responseHeaderLength(message: HttpResponse): Int = {
    message.getProtocolVersion.toString.getBytes("UTF-8").length + 1 + message.getStatus.toString.getBytes("UTF-8").length + NEWLINE_LENGTH + httpHeadersLength(message)
  }
  final def httpHeadersLength(message: HttpMessage): Int = {
    import scala.collection.JavaConversions._
    message.getHeaders.foldLeft(NEWLINE_LENGTH) {
      (s, v) ⇒ s + v.getKey.getBytes("UTF-8").length + SEPERATOR_LENGTH + v.getValue.getBytes("UTF-8").length + NEWLINE_LENGTH
    }
  }
  final def chunkedHeaderLength(message: HttpChunk): Int = {
    message.getContent.readableBytes().toHexString.getBytes("UTF-8").length + NEWLINE_LENGTH
  }

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
