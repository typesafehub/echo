/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */

package com.typesafe.atmos.trace;

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
import com.typesafe.atmos.util.Uuid;

privileged public aspect RemoteTraceAspect {

  // ----------------------------------------------------
  // Tracer attached to actor ref provider
  // ----------------------------------------------------

  declare parents: RemoteActorRefProvider implements WithTracer;

  private volatile ActorSystemTracer RemoteActorRefProvider._atmos$tracer;

  private ActorSystemTracer RemoteActorRefProvider.atmos$tracer() {
    return _atmos$tracer;
  }

  private void RemoteActorRefProvider.atmos$tracer(ActorSystemTracer tracer) {
    _atmos$tracer = tracer;
  }

  public Tracer RemoteActorRefProvider.tracer() {
    return (Tracer) _atmos$tracer;
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
    provider.atmos$tracer((ActorSystemTracer) system.tracer());
  }

  // ----------------------------------------------------
  // Tracer attached to remote transport
  // ----------------------------------------------------

  private volatile ActorSystemTracer RemoteTransport._atmos$tracer;

  private ActorSystemTracer RemoteTransport.atmos$tracer() {
    return _atmos$tracer;
  }

  private void RemoteTransport.atmos$tracer(ActorSystemTracer tracer) {
    _atmos$tracer = tracer;
  }

  // attach the tracer to remote transport

  before(RemoteTransport transport, ActorSystemImpl system, RemoteActorRefProvider provider):
    execution(akka.remote.RemoteTransport+.new(..)) &&
    this(transport) &&
    args(system, provider)
  {
    transport.atmos$tracer((ActorSystemTracer) system.tracer());
  }

  // ----------------------------------------------------
  // Tracing metadata attached to remote actor ref
  // ----------------------------------------------------

  before(RemoteActorRef actorRef, RemoteActorRefProvider provider, RemoteTransport remote, ActorPath path):
    execution(akka.remote.RemoteActorRef.new(..)) &&
    this(actorRef) &&
    args(provider, remote, path, ..)
  {
    ActorSystemTracer tracer = provider.atmos$tracer();
    actorRef.atmos$tracer(tracer);
    if (enabled(tracer)) {
      String identifier = tracer.actor().identifier(path);
      actorRef.atmos$identifier(identifier);
      boolean traceable = tracer.actor().traceable(identifier);
      actorRef.atmos$traceable(traceable);
      if (traceable) {
        ActorInfo info = tracer.actor().info(path, null, true);
        actorRef.atmos$info(info);
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
    ActorSystemTracer tracer = actorRef.atmos$tracer();
    if (enabled(tracer)) {
      if (actorRef.atmos$traceable()) {
        TraceContext context = tracer.actor().message().sysMsgDispatched(actorRef.atmos$info(), message);
        tracer.trace().local().start(context);
        TraceContext remoteContext = tracer.remote().sent(actorRef.atmos$info(), message, builder.getMessage().getSerializedSize());
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
    ActorSystemTracer tracer = actorRef.atmos$tracer();
    if (enabled(tracer)) {
      if (message instanceof Failed) {
        if (sender != null && sender.atmos$traceable()) {
          TraceContext context = tracer.actor().failed(sender.atmos$info(), ((Failed) message).cause(), actorRef.atmos$info());
          tracer.trace().local().start(context);
          TraceContext remoteContext = tracer.remote().sent(actorRef.atmos$info(), message, builder.getMessage().getSerializedSize());
          tracer.trace().local().end();
          RemoteTrace.attachTraceContext(builder, remoteContext);
        }
      } else if (actorRef.atmos$traceable()) {
        ActorInfo senderInfo = (sender != null && sender.atmos$traceable()) ? sender.atmos$info() : null;
        TraceContext context = tracer.actor().told(actorRef.atmos$identifier(), actorRef.atmos$info(), message, senderInfo);
        tracer.trace().local().start(context);
        TraceContext remoteContext = tracer.remote().sent(actorRef.atmos$info(), message, builder.getMessage().getSerializedSize());
        tracer.trace().local().end();
        RemoteTrace.attachTraceContext(builder, remoteContext);
      }
    }
  }

  // ----------------------------------------------------
  // Extract trace context in remote message
  // ----------------------------------------------------

  private volatile TraceContext RemoteMessage._atmos$trace = TraceContext.ZeroTrace();

  private TraceContext RemoteMessage.atmos$trace() {
    return _atmos$trace;
  }

  private void RemoteMessage.atmos$trace(TraceContext context) {
    _atmos$trace = context;
  }

  private volatile int RemoteMessage._atmos$size = 0;

  private int RemoteMessage.atmos$size() {
    return _atmos$size;
  }

  private void RemoteMessage.atmos$size(int size) {
    _atmos$size = size;
  }

  after(RemoteMessage message, RemoteMessageProtocol input, ActorSystemImpl system):
    execution(akka.remote.RemoteMessage.new(..)) &&
    this(message) &&
    args(input, system)
  {
    message.atmos$trace(RemoteTrace.extractTraceContext(input));
    message.atmos$size(input.getMessage().getSerializedSize());
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
    ActorSystemTracer tracer = actorRef.atmos$tracer();

    if (disabled(tracer) || !actorRef.atmos$traceable()) return proceed(remote, remoteMessage);

    TraceContext context = remoteMessage.atmos$trace();

    if (Uuid.isZero(context.trace())) return proceed(remote, remoteMessage);

    ActorInfo info = actorRef.atmos$info();
    Object message = remoteMessage.payload();

    tracer.trace().local().start(context);
    tracer.remote().received(info, message, remoteMessage.atmos$size());
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
    ActorSystemTracer tracer = transport.atmos$tracer();
    if (enabled(tracer)) {
      tracer.remote().status(event);
    }
  }
}
