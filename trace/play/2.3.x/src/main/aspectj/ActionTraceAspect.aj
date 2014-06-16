/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */

package com.typesafe.trace;

import java.lang.System;
import play.api.GlobalSettings;
import play.api.libs.iteratee.Enumeratee;
import play.api.libs.iteratee.Iteratee;
import play.api.mvc.Action;
import play.api.mvc.ActionBuilder;
import play.api.mvc.BodyParser;
import play.api.mvc.Handler;
import play.api.mvc.Request;
import play.api.mvc.RequestHeader;
import play.api.mvc.Results;
import play.core.Router.HandlerDef;
import play.core.Router.HandlerInvoker;
import play.core.Router.Routes.TaggingInvoker;
import play.core.Router;
import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;
import scala.Function0;
import scala.Function1;
import scala.Option;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;

privileged aspect ActionTraceAspect {

  private volatile boolean RequestHeader._echo$generationStartSent = false;

  public boolean RequestHeader.echo$generationStartSent() {
    return _echo$generationStartSent;
  }

  public void RequestHeader.echo$generationStartSent(boolean generationStartSent) {
    _echo$generationStartSent = generationStartSent;
  }

  private volatile HandlerDef TaggingInvoker._echo$handlerDef = null;

  public HandlerDef TaggingInvoker.echo$handlerDef() {
    return _echo$handlerDef;
  }

  public void TaggingInvoker.echo$handlerDef(HandlerDef handlerDef) {
    _echo$handlerDef = handlerDef;
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

  after(ChannelHandlerContext ctx,MessageEvent e):
    execution(public void play.core.server.netty.PlayDefaultUpstreamHandler+.messageReceived(ChannelHandlerContext,MessageEvent)) &&
    args(ctx, e)
  {
    ActionTracer tracer = ActionTracer.global();
    if (tracing(tracer)) {
      Channel channel = ctx.getChannel();
      channel.echo$context(null);
    }
  }

  before(Action action, RequestHeader request):
    execution(public Iteratee play.api.mvc.Action+.apply(RequestHeader)) &&
    this(action) &&
    args(request)
  {
    ActionTracer tracer = ActionTracer.global();
    if (tracing(tracer)) {
      tracer.action().invoked(request);
    }
  }

  Request around(play.api.mvc.Request$ self, RequestHeader request, Object arg):
    execution(public Request play.api.mvc.Request$.apply(RequestHeader,Object)) &&
    this(self) &&
    args(request, arg)
  {
    ActionTracer tracer = ActionTracer.global();
    if (tracing(tracer)) {
      request.echo$generationStartSent(true);
      Request r = proceed(self,request,arg);
      r.echo$generationStartSent(true);
      return r;
    } else
      return proceed(self,request,arg);
  }

  before(RequestHeader request, Throwable exception):
    execution(public Future play.api.GlobalSettings+.onError(RequestHeader,Throwable)) &&
    args(request, exception)
  {
    ActionTracer tracer = ActionTracer.global();
    if (tracing(tracer)) {
      tracer.action().error(request,exception);
    }
  }

  before(RequestHeader request):
    execution(public Future play.api.GlobalSettings+.onHandlerNotFound(RequestHeader)) &&
    args(request)
  {
    ActionTracer tracer = ActionTracer.global();
    if (tracing(tracer)) {
      tracer.action().handlerNotFound(request);
    }
  }

  before(RequestHeader request, String error):
    execution(public Future play.api.GlobalSettings+.onBadRequest(RequestHeader, String)) &&
    args(request, error)
  {
    ActionTracer tracer = ActionTracer.global();
    if (tracing(tracer)) {
      if (!request.echo$generationStartSent()) {
        tracer.action().resultGenerationStart();
        request.echo$generationStartSent(true);
      }
      tracer.action().badRequest(request,error);
    }
  }

 /*  Changed execution->call on trace of onRouteRequest
  *
  *  Seems to be a cause of trouble for some Play applications that use
  *  javax.* classes.  Not exactly sure why, but followed suggestion found
  *  here: http://www.eclipse.org/forums/index.php/t/206028/
  *
  *  Converting the 'execution' to a 'call' does not seem to have affected
  *  the tests - all pass.  But it does now cause echo to weave properly into
  *  this test application: https://github.com/andyczerwonka/tyrion
  *
  *  Without this change the aspectj weaver would issue a warning:
  *
  *  "warning javax.* types are not being woven because the weaver option '-Xset:weaveJavaxPackages=true' has not been specified"
  *
  *  Would be displayed and a single cutpoint:
  *  public Option play.api.GlobalSettings+.onRouteRequest(RequestHeader)
  *
  *  Would fail.
  */
  after(RequestHeader request) returning(Option handler):
    call(public Option play.api.GlobalSettings+.onRouteRequest(RequestHeader)) &&
    args(request)
  {
    ActionTracer tracer = ActionTracer.global();
    if (tracing(tracer)) {
      tracer.action().routeRequest(request,handler);
    }
  }

  before(play.core.Router.Routes.TaggingInvoker self, HandlerInvoker underlyingInvoker, HandlerDef handlerDef):
    execution(play.core.Router.Routes.TaggingInvoker.new(..)) &&
    this(self) &&
    args(.., underlyingInvoker, handlerDef)
  {
    ActionTracer tracer = ActionTracer.global();
    if (tracing(tracer)) {
      self.echo$handlerDef(handlerDef);
    }
  }


  before(play.core.Router.Routes.TaggingInvoker self, Function0 func0):
    execution(public Handler play.core.Router$Routes$TaggingInvoker.call(Function0, ..)) &&
    this(self) &&
    args(func0, ..)
  {
    ActionTracer tracer = ActionTracer.global();
    HandlerDef handlerDef = self.echo$handlerDef();
    if (tracing(tracer)) {
      tracer.action().resolved(handlerDef.controller, handlerDef.method, handlerDef.parameterTypes, handlerDef.verb, handlerDef.comments, handlerDef.path);
    }
  }

  before():
    execution(public Future play.api.mvc.Action+.apply(Request))
  {
    ActionTracer tracer = ActionTracer.global();
    if (tracing(tracer)) {
      tracer.action().resultGenerationStart();
    }
  }

  after(Results results) returning(Enumeratee r):
    execution(public Enumeratee play.api.mvc.Results+.chunk(Option)) &&
    this(results)
  {
    ActionTracer tracer = ActionTracer.global();
    if (tracing(tracer)) {
      r.echo$setChunking();
    }
  }

  Object around(play.core.server.netty.RequestBodyHandler helper, Iteratee iteratee, Function1 handlerFunc, Function0 finish):
    execution(public Future play.core.server.netty.RequestBodyHandler+.newRequestBodyUpstreamHandler(Iteratee,Function1,Function0)) &&
    this(helper) &&
    args(iteratee,handlerFunc,finish)
  {
    ActionTracer tracer = ActionTracer.global();
    if (tracing(tracer)) {
      return proceed(helper,iteratee,ActionTrace.simpleChannelUpstreamHandlerProxyFunc(tracer,handlerFunc),finish);
    } else
      return proceed(helper,iteratee,handlerFunc,finish);
  }

}
