/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */

package com.typesafe.trace;

import java.lang.System;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.HttpChunk;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpMessage;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;
import play.core.server.netty.PlayDefaultUpstreamHandler;
import play.core.server.netty.RequestBodyHandler;

privileged aspect NettyTraceAspect {

  private volatile TraceContext Channel._echo$context;

  public TraceContext Channel.echo$context() {
    return _echo$context;
  }

  public void Channel.echo$context(TraceContext context) {
    _echo$context = context;
  }

  private volatile NettyTrace.DeferredData HttpRequestDecoder._echo$deferredData;

  public NettyTrace.DeferredData HttpRequestDecoder.echo$deferredData() {
    return _echo$deferredData;
  }

  public void HttpRequestDecoder.echo$deferredData(NettyTrace.DeferredData deferred) {
    _echo$deferredData = deferred;
  }

  public boolean enabled(ActionTracer tracer) {
    return tracer != null && tracer.enabled();
  }

  public boolean disabled(ActionTracer tracer) {
    return tracer == null || !tracer.enabled();
  }

  public boolean tracing(ActionTracer tracer) {
    return enabled(tracer) && tracer.trace().tracing();
  }

  // Useful when debugging
  // public static String bytesToHex(byte[] bytes) {
  //   final char[] hexArray = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
  //   char[] hexChars = new char[bytes.length * 2];
  //   int v;
  //   for ( int j = 0; j < bytes.length; j++ ) {
  //     v = bytes[j] & 0xFF;
  //     hexChars[j * 2] = hexArray[v >>> 4];
  //     hexChars[j * 2 + 1] = hexArray[v & 0x0F];
  //   }
  //   return new String(hexChars);
  // }

  // Channel instrumentation

  Object around(HttpResponseEncoder encoder, ChannelHandlerContext ctx, Channel channel, Object msg):
    execution(protected Object org.jboss.netty.handler.codec.http.HttpMessageEncoder+.encode(ChannelHandlerContext,Channel,Object)) &&
    this(encoder) &&
    args(ctx,channel,msg)
  {
    ActionTracer tracer = ActionTracer.global();
    if (tracing(tracer)) {
      if (msg instanceof HttpChunk) {
        HttpChunk chunk = (HttpChunk) msg;
        ChannelBuffer buffer = chunk.getContent();
        Object r = proceed(encoder,ctx,channel,msg);
        if (r instanceof ChannelBuffer) {
          ChannelBuffer cb = (ChannelBuffer) r;
          tracer.netty().writeChunk(NettyTrace.chunkedHeaderLength(chunk),(int)buffer.readableBytes());
        } else {
         // System.out.println("-- chunk of size: "+buffer.writerIndex()+" returned message is: "+r);
        }
        return r;
      } else if (msg instanceof HttpResponse) {
        tracer.action().resultGenerationEnd();
        HttpResponse response = (HttpResponse) msg;
        long length = HttpHeaders.getContentLength(response);
        Object r = proceed(encoder,ctx,channel,msg);
        if (r instanceof ChannelBuffer) {
          ChannelBuffer cb = (ChannelBuffer) r;
          tracer.netty().responseHeader(NettyTrace.responseHeaderLength(response));
          if (!response.isChunked()) {
            tracer.action().simpleResult(response.getStatus().getCode());
            tracer.netty().responseBody((int)length);
          } else {
            tracer.action().chunkedResult(response.getStatus().getCode());
          }
        } else {
         // System.out.println("-- response of size: "+length+" returned message is: "+r);
        }
        return r;
      } else {
       // System.out.println("writing an object: "+msg);
        return proceed(encoder,ctx,channel,msg);
      }
    } else
       return proceed(encoder,ctx,channel,msg);
  }

  after(HttpRequestDecoder decoder, String[] lines) returning(HttpMessage message):
    execution(protected HttpMessage org.jboss.netty.handler.codec.http.HttpRequestDecoder+.createMessage(String[])) &&
    this(decoder) &&
    args(lines)
  {
    ActionTracer tracer = ActionTracer.global();
    if (tracing(tracer)) {
      HttpRequest req = (HttpRequest) message;
      NettyTrace.DeferredData deferred = decoder.echo$deferredData();
      String reqUri = req.getUri();
      TraceContext context = tracer.netty().traceUriContext(reqUri);
      if (context == null) {
        context = tracer.netty().httpReceivedStart();
        deferred.channel().echo$context(context);
      } else {
        deferred.channel().echo$context(null);
      }
      tracer.trace().local().start(context);
      tracer.netty().readBytes(deferred.readBytes);
    }
  }

  void around(HttpRequestDecoder decoder, ChannelHandlerContext ctx, MessageEvent event):
    execution(public void org.jboss.netty.handler.codec.replay.ReplayingDecoder+.messageReceived(ChannelHandlerContext, MessageEvent)) &&
    this(decoder) &&
    args(ctx,event)
  {
    ActionTracer tracer = ActionTracer.global();
    if (tracing(tracer)) {
      boolean writeEnd = true;
      ChannelHandler ch = ctx.getPipeline().get("handler");
      ChannelBuffer cb = (ChannelBuffer) event.getMessage();
      Channel channel = ctx.getChannel();
      TraceContext context = null;
      boolean isSimpleChannelUpstreamHandlerProxy = (ch instanceof ActionTrace.SimpleChannelUpstreamHandlerProxy);
      if (isSimpleChannelUpstreamHandlerProxy) {
        ActionTrace.SimpleChannelUpstreamHandlerProxy proxy = (ActionTrace.SimpleChannelUpstreamHandlerProxy) ch;
        context = proxy.context();
      } else {
        if (channel.echo$context() == null) {
          decoder.echo$deferredData(NettyTrace.deferredData(channel,(int) cb.readableBytes()));
        } else {
          context = channel.echo$context();
          writeEnd = false;
        }
      }
      if (context != null) {
        tracer.trace().local().start(context);
        int rb = (int) cb.readableBytes();
        tracer.netty().readBytes(rb);
      }
      proceed(decoder,ctx,event);
      decoder.echo$deferredData(null);
      if (!isSimpleChannelUpstreamHandlerProxy && writeEnd) {
        tracer.netty().httpReceivedEnd();
      }
      // It may appear that based on the above code that a trace context is not always started, but
      // There is another call to a local().start(...) inside the call to `proceed` because of the
      // aspect on `org.jboss.netty.handler.codec.http.HttpRequestDecoder.createMessage`.  The claim
      // is that all pathways result in a local context being created.
      tracer.trace().local().end();
    } else
      proceed(decoder,ctx,event);
  }

  void around(PlayDefaultUpstreamHandler handler, ChannelHandlerContext ctx, MessageEvent event):
    execution(public void play.core.server.netty.PlayDefaultUpstreamHandler.messageReceived(ChannelHandlerContext, MessageEvent)) &&
    this(handler) &&
    args(ctx, event)
  {
    ActionTracer tracer = ActionTracer.global();
    if (tracing(tracer)) {
      Channel c = event.getChannel();
      tracer.netty().playReceivedStart();
      proceed(handler,ctx,event);
      tracer.netty().playReceivedEnd();
    } else
      proceed(handler,ctx,event);
  }
}
