/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */

package com.typesafe.trace;

import akka.actor.ActorPath;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.ActorSystemImpl;
import akka.actor.Failed;
import akka.actor.InternalActorRef;
import akka.dispatch.SystemMessage;
import akka.remote.RemoteActorRef;
import akka.remote.RemoteActorRefProvider;
import akka.remote.RemoteLifeCycleEvent;
import akka.remote.RemoteMessage;
import akka.remote.RemoteProtocol.RemoteMessageProtocol;
import akka.remote.RemoteSettings;
import akka.remote.RemoteTransport;
import com.typesafe.trace.util.Uuid;

privileged public aspect RemoteTraceAspect {

  // ----------------------------------------------------
  // Tracer attached to actor ref provider
  // ----------------------------------------------------

  declare parents: RemoteActorRefProvider implements WithTracer;

  private volatile ActorSystemTracer RemoteActorRefProvider._echo$tracer;

  private ActorSystemTracer RemoteActorRefProvider.echo$tracer() {
    return _echo$tracer;
  }

  private void RemoteActorRefProvider.echo$tracer(ActorSystemTracer tracer) {
    _echo$tracer = tracer;
  }

  public Tracer RemoteActorRefProvider.tracer() {
    return (Tracer) _echo$tracer;
  }

  public boolean enabled(ActorSystemTracer tracer) {
    return tracer != null && tracer.enabled();
  }

  public boolean disabled(ActorSystemTracer tracer) {
    return tracer == null || !tracer.enabled();
  }

  // attach the tracer to remote actor ref provider

  before(RemoteActorRefProvider provider, ActorSystemImpl system):
    execution(* akka.remote.RemoteActorRefProvider.init(..)) &&
    this(provider) &&
    args(system)
  {
    provider.echo$tracer((ActorSystemTracer) system.tracer());
  }

  // ----------------------------------------------------
  // Tracer attached to remote transport
  // ----------------------------------------------------

  private volatile ActorSystemTracer RemoteTransport._echo$tracer;

  private ActorSystemTracer RemoteTransport.echo$tracer() {
    return _echo$tracer;
  }

  private void RemoteTransport.echo$tracer(ActorSystemTracer tracer) {
    _echo$tracer = tracer;
  }

  // attach the tracer to remote transport

  before(RemoteTransport transport, ActorSystemImpl system, RemoteActorRefProvider provider):
    execution(akka.remote.RemoteTransport+.new(..)) &&
    this(transport) &&
    args(system, provider)
  {
    transport.echo$tracer((ActorSystemTracer) system.tracer());
  }

  // ----------------------------------------------------
  // Tracing metadata attached to remote actor ref
  // ----------------------------------------------------

  before(RemoteActorRef actorRef, RemoteActorRefProvider provider, RemoteTransport remote, ActorPath path):
    execution(akka.remote.RemoteActorRef.new(..)) &&
    this(actorRef) &&
    args(provider, remote, path, ..)
  {
    ActorSystemTracer tracer = provider.echo$tracer();
    actorRef.echo$tracer(tracer);
    if (enabled(tracer)) {
      String identifier = tracer.actor().identifier(path);
      actorRef.echo$identifier(identifier);
      boolean traceable = tracer.actor().traceable(identifier);
      actorRef.echo$traceable(traceable);
      if (traceable) {
        ActorInfo info = tracer.actor().info(path, null, true);
        actorRef.echo$info(info);
      }
    }
  }

  // ----------------------------------------------------
  // Remote system message send tracing
  // ----------------------------------------------------

  after(ActorRef actorRef, SystemMessage message)
  returning(RemoteMessageProtocol.Builder builder):
    execution(* akka.remote.RemoteTransport.createRemoteMessageProtocolBuilder(..)) &&
    cflow(execution(* akka.remote.RemoteActorRef.sendSystemMessage(..)) && this(actorRef) && args(message))
  {
    ActorSystemTracer tracer = actorRef.echo$tracer();
    if (enabled(tracer)) {
      if (actorRef.echo$traceable()) {
        TraceContext context = tracer.actor().message().sysMsgDispatched(actorRef.echo$info(), message);
        tracer.trace().local().start(context);
        TraceContext remoteContext = tracer.remote().sent(actorRef.echo$info(), message, builder.getMessage().getSerializedSize());
        tracer.trace().local().end();
        RemoteTrace.attachTraceContext(builder, remoteContext);
      }
    }
  }

  // ----------------------------------------------------
  // Remote message send tracing
  // ----------------------------------------------------

  after(ActorRef actorRef, Object message, ActorRef sender)
  returning(RemoteMessageProtocol.Builder builder):
    execution(* akka.remote.RemoteTransport.createRemoteMessageProtocolBuilder(..)) &&
    cflow(execution(* akka.remote.RemoteActorRef.$bang(..)) && this(actorRef) && args(message, sender))
  {
    ActorSystemTracer tracer = actorRef.echo$tracer();
    if (enabled(tracer)) {
      if (message instanceof Failed) {
        if (sender != null && sender.echo$traceable()) {
          TraceContext context = tracer.actor().failed(sender.echo$info(), ((Failed) message).cause(), actorRef.echo$info());
          tracer.trace().local().start(context);
          TraceContext remoteContext = tracer.remote().sent(actorRef.echo$info(), message, builder.getMessage().getSerializedSize());
          tracer.trace().local().end();
          RemoteTrace.attachTraceContext(builder, remoteContext);
        }
      } else if (actorRef.echo$traceable()) {
        ActorInfo senderInfo = (sender != null && sender.echo$traceable()) ? sender.echo$info() : null;
        TraceContext context = tracer.actor().told(actorRef.echo$identifier(), actorRef.echo$info(), message, senderInfo);
        tracer.trace().local().start(context);
        TraceContext remoteContext = tracer.remote().sent(actorRef.echo$info(), message, builder.getMessage().getSerializedSize());
        tracer.trace().local().end();
        RemoteTrace.attachTraceContext(builder, remoteContext);
      }
    }
  }

  // ----------------------------------------------------
  // Extract trace context in remote message
  // ----------------------------------------------------

  private volatile TraceContext RemoteMessage._echo$trace = TraceContext.ZeroTrace();

  private TraceContext RemoteMessage.echo$trace() {
    return _echo$trace;
  }

  private void RemoteMessage.echo$trace(TraceContext context) {
    _echo$trace = context;
  }

  private volatile int RemoteMessage._echo$size = 0;

  private int RemoteMessage.echo$size() {
    return _echo$size;
  }

  private void RemoteMessage.echo$size(int size) {
    _echo$size = size;
  }

  after(RemoteMessage message, RemoteMessageProtocol input, ActorSystemImpl system):
    execution(akka.remote.RemoteMessage.new(..)) &&
    this(message) &&
    args(input, system)
  {
    message.echo$trace(RemoteTrace.extractTraceContext(input));
    message.echo$size(input.getMessage().getSerializedSize());
  }

  // ----------------------------------------------------
  // Remote message processing tracing
  // ----------------------------------------------------

  Object around(RemoteTransport remote, RemoteMessage remoteMessage):
    execution(* akka.remote.RemoteTransport.receiveMessage(..)) &&
    this(remote) &&
    args(remoteMessage)
  {
    ActorRef actorRef = (ActorRef) remoteMessage.recipient();
    ActorSystemTracer tracer = actorRef.echo$tracer();

    if (disabled(tracer) || !actorRef.echo$traceable()) return proceed(remote, remoteMessage);

    TraceContext context = remoteMessage.echo$trace();

    if (Uuid.isZero(context.trace())) return proceed(remote, remoteMessage);

    ActorInfo info = actorRef.echo$info();
    Object message = remoteMessage.payload();

    tracer.trace().local().start(context);
    tracer.remote().received(info, message, remoteMessage.echo$size());
    Object result = proceed(remote, remoteMessage);
    tracer.remote().completed(info, message);
    tracer.trace().local().end();
    return result;
  }

  // ----------------------------------------------------
  // Remote life-cycle event tracing
  // ----------------------------------------------------

  before(RemoteTransport transport, RemoteLifeCycleEvent event):
    execution(* akka.remote.RemoteTransport.notifyListeners(..)) &&
    this(transport) &&
    args(event)
  {
    ActorSystemTracer tracer = transport.echo$tracer();
    if (enabled(tracer)) {
      tracer.remote().status(event);
    }
  }
}
