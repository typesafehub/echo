/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */

package com.typesafe.atmos.trace

import play.api.mvc.{ RequestHeader, ChunkedResult, SimpleResult, AsyncResult, Handler, WebSocket, EssentialAction }
import play.api.Routes
import org.jboss.netty.channel.{ SimpleChannelUpstreamHandler, ChannelHandlerContext, MessageEvent, ExceptionEvent, ChannelStateEvent }

private[trace] object ActionTrace {
  class SimpleChannelUpstreamHandlerProxy(tracer: ActionTracer, val context: TraceContext, underlying: SimpleChannelUpstreamHandler) extends SimpleChannelUpstreamHandler {
    override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent): Unit = {
      tracer.trace.local.start(context)
      tracer.action.chunkedInputStart
      underlying.messageReceived(ctx, e)
      tracer.action.chunkedInputEnd
      tracer.trace.local.end
    }
    override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent): Unit = {
      underlying.exceptionCaught(ctx, e)
    }
    override def channelDisconnected(ctx: ChannelHandlerContext, e: ChannelStateEvent): Unit = {
      underlying.channelDisconnected(ctx, e)
    }
  }

  def simpleChannelUpstreamHandlerProxy(tracer: ActionTracer, underlying: SimpleChannelUpstreamHandler): SimpleChannelUpstreamHandler =
    new SimpleChannelUpstreamHandlerProxy(tracer, tracer.trace.continue(), underlying)

  def simpleChannelUpstreamHandlerProxyFunc(tracer: ActionTracer, func: SimpleChannelUpstreamHandler ⇒ Unit): SimpleChannelUpstreamHandler ⇒ Unit = {
    underlying: SimpleChannelUpstreamHandler ⇒
      func(simpleChannelUpstreamHandlerProxy(tracer, underlying))
  }
}

/**
 * Tracing events of Actions.
 */
private[trace] class ActionTrace(val trace: Trace) extends WithTracing {
  import ActionTrace._

  lazy val actionEvents = trace.settings.events.actions

  lazy val maxStackTraceSize = trace.settings.maxStackTraceSize

  def enabled: Boolean = actionEvents && trace.tracing

  def resolved(controller: String, method: String, parameterTypes: Seq[Class[_]], verb: String, comments: String, path: String): Unit =
    withTE(ActionResolved(
      ActionResolvedInfo(controller = controller,
        method = method,
        parameterTypes = parameterTypes.map(_.toString),
        verb = verb,
        comments = trace.formatLongMessage(comments),
        path = path)))

  def invoked(r: RequestHeader): Unit =
    withTE(ActionInvoked(
      ActionInvocationInfo(r.tags.get(Routes.ROUTE_CONTROLLER).getOrElse(PlayTracing.NO_CONTROLLER),
        r.tags.get(Routes.ROUTE_ACTION_METHOD).getOrElse(PlayTracing.NO_METHOD),
        r.tags.get(Routes.ROUTE_PATTERN).getOrElse(PlayTracing.NO_PATTERN),
        r.id,
        r.uri,
        r.path,
        r.method,
        r.version,
        r.remoteAddress,
        if (r.host.isEmpty) None else Some(r.host),
        if (r.domain.isEmpty) None else Some(r.domain),
        if (r.session.data.isEmpty) None else Some(r.session.data))))

  private def toActionRequestInfo(rh: RequestHeader): ActionRequestInfo =
    ActionRequestInfo(rh.id,
      rh.tags,
      rh.uri,
      rh.path,
      rh.method,
      rh.version,
      rh.queryString,
      rh.headers.toMap)

  def routeRequest(rh: RequestHeader, handler: Option[Handler]): Unit = {
    withTE(ActionRouteRequest(toActionRequestInfo(rh),
      handler match {
        case None                  ⇒ ActionRouteRequestResult.NoHandler
        case Some(_: WebSocket[_]) ⇒ ActionRouteRequestResult.WebSocket
        case Some(_)               ⇒ ActionRouteRequestResult.EssentialAction
      }))
  }

  def error(rh: RequestHeader, exception: Throwable): Unit = {
    withTE(ActionError(toActionRequestInfo(rh),
      exception.getMessage,
      exception.getCause match {
        case null             ⇒ exception.getStackTrace.take(maxStackTraceSize).map(_.toString)
        case cause: Throwable ⇒ cause.getStackTrace.take(maxStackTraceSize).map(_.toString)
      }))
  }

  def handlerNotFound(rh: RequestHeader): Unit = {
    withTE(ActionHandlerNotFound(toActionRequestInfo(rh)))
  }

  def badRequest(rh: RequestHeader, error: String): Unit = {
    withTE(ActionBadRequest(toActionRequestInfo(rh), error))
  }

  def resultGenerationStart(): Unit =
    withTE(ActionResultGenerationStart)

  def resultGenerationEnd(): Unit =
    withTE(ActionResultGenerationEnd)

  def asyncResult(): Unit =
    withTE(ActionAsyncResult)

  def chunkedInputStart(): Unit =
    withTE(ActionChunkedInputStart)

  def chunkedInputEnd(): Unit =
    withTE(ActionChunkedInputEnd)

  def simpleResult(httpResponseCode: Int): Unit =
    withTE(ActionSimpleResult(ActionResultInfo(httpResponseCode)))

  def chunkedResult(httpResponseCode: Int): Unit =
    withTE(ActionChunkedResult(ActionResultInfo(httpResponseCode)))

}
