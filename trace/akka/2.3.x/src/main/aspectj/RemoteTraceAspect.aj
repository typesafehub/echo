/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */

package com.typesafe.trace;

import akka.actor.ActorPath;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.ActorSystemImpl;
import akka.actor.Address;
import akka.actor.FSM.Event;
import akka.actor.InternalActorRef;
import akka.dispatch.sysmsg.Failed;
import akka.dispatch.sysmsg.SystemMessage;
import akka.remote.Ack;
import akka.remote.EndpointManager.Send;
import akka.remote.EventPublisher;
import akka.remote.RemoteActorRef;
import akka.remote.RemoteActorRefProvider;
import akka.remote.RemoteSettings;
import akka.remote.RemoteTransport;
import akka.remote.RemotingLifecycleEvent;
import akka.remote.transport.AkkaPduCodec.Message;
import akka.remote.transport.AkkaPduCodec;
import akka.remote.WireFormats.SerializedMessage;
import akka.util.ByteString;
import com.typesafe.trace.util.Uuid;
import scala.Option;
import scala.Tuple2;

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

  before(RemoteActorRef actorRef, RemoteTransport remote, Address localAddressToUse, ActorPath path):
    execution(akka.remote.RemoteActorRef.new(..)) &&
    this(actorRef) &&
    args(remote, localAddressToUse, path, ..)
  {
    ActorSystemTracer tracer = remote.echo$tracer();
    actorRef.echo$tracer(tracer);
    if (enabled(tracer)) {
      String identifier = tracer.actor().identifier(path);
      actorRef.echo$identifier(identifier);
      boolean traceable = tracer.actor().traceable(identifier);
      actorRef.echo$traceable(traceable);
      if (traceable) {
        ActorInfo info = tracer.actor().info(path, null, true, false);
        actorRef.echo$info(info);
      }
    }
  }

  // ----------------------------------------------------
  // Transfer trace context with remote send
  // ----------------------------------------------------

  private volatile TraceContext Send._echo$trace = TraceContext.ZeroTrace();

  private TraceContext Send.echo$trace() {
    return _echo$trace;
  }

  private void Send.echo$trace(TraceContext context) {
    _echo$trace = context;
  }

  // ----------------------------------------------------
  // Copy trace context over with Send.copy
  // ----------------------------------------------------

  after(Send send) returning(Send newSend):
    execution(* akka.remote.EndpointManager.Send.copy(..)) &&
    this(send)
  {
    newSend.echo$trace(send.echo$trace());
  }

  // ----------------------------------------------------
  // Remote system message send tracing
  // ----------------------------------------------------

  before(Send send, Object messageObject, Option<ActorRef> senderOption, RemoteActorRef recipient):
    execution(akka.remote.EndpointManager.Send.new(..)) &&
    this(send) &&
    args(messageObject, senderOption, recipient, ..) &&
    cflow(execution(* akka.remote.RemoteActorRef.sendSystemMessage(..)))
  {
    ActorSystemTracer tracer = recipient.echo$tracer();
    if (enabled(tracer) && messageObject instanceof SystemMessage) {
      SystemMessage message = (SystemMessage) messageObject;
      if (message instanceof Failed) {
        Failed failed = (Failed) message;
        ActorRef child = failed.child();
        if (child != null && child.echo$traceable()) {
          TraceContext context = tracer.actor().failed(child.echo$info(), failed.cause(), recipient.echo$info());
          send.echo$trace(context);
        }
      } else if (recipient.echo$traceable()) {
        TraceContext context = tracer.actor().message().sysMsgDispatched(recipient.echo$info(), message);
        send.echo$trace(context);
      }
    }
  }

  // ----------------------------------------------------
  // Remote message send tracing
  // ----------------------------------------------------

  before(Send send, Object message, Option<ActorRef> senderOption, RemoteActorRef recipient):
    execution(akka.remote.EndpointManager.Send.new(..)) &&
    this(send) &&
    args(message, senderOption, recipient, ..) &&
    cflow(execution(* akka.remote.RemoteActorRef.$bang(..)))
  {
    ActorSystemTracer tracer = recipient.echo$tracer();
    if (enabled(tracer) && recipient.echo$traceable()) {
      ActorInfo senderInfo = (senderOption.isDefined() && senderOption.get().echo$traceable()) ? senderOption.get().echo$info() : null;
      TraceContext context = tracer.actor().told(recipient.echo$identifier(), recipient.echo$info(), message, senderInfo);
      send.echo$trace(context);
    }
  }

  // ----------------------------------------------------
  // Remote send tracing
  // ----------------------------------------------------

  ByteString around(AkkaPduCodec codec, Address localAddress, ActorRef recipient, SerializedMessage serializedMessage, Event fsmEvent):
    execution(* akka.remote.transport.AkkaPduCodec+.constructMessage(..)) &&
    this(codec) &&
    args(localAddress, recipient, serializedMessage, ..) &&
    cflow(execution (* akka.remote.EndpointWriter.processEvent(..)) && args(fsmEvent, ..))
  {
    ActorSystemTracer tracer = recipient.echo$tracer();
    Object event = fsmEvent.event();
    if (enabled(tracer) && recipient.echo$traceable() && event instanceof Send) {
      Send send = (Send) event;
      tracer.trace().local().start(send.echo$trace());
      TraceContext context = tracer.remote().sent(recipient.echo$info(), send.message(), serializedMessage.getSerializedSize());
      tracer.trace().local().end();
      ByteString bytes = proceed(codec, localAddress, recipient, serializedMessage, fsmEvent);
      return RemoteTrace.attachTraceContext(bytes, context);
    } else {
      return proceed(codec, localAddress, recipient, serializedMessage, fsmEvent);
    }
  }

  // ----------------------------------------------------
  // Extract trace context in remote message
  // ----------------------------------------------------

  private volatile TraceContext SerializedMessage._echo$trace = TraceContext.ZeroTrace();

  private TraceContext SerializedMessage.echo$trace() {
    return _echo$trace;
  }

  private void SerializedMessage.echo$trace(TraceContext context) {
    _echo$trace = context;
  }

  Tuple2<Option<Ack>, Option<Message>> around(ByteString raw):
    execution(* akka.remote.transport.AkkaPduCodec+.decodeMessage(..)) &&
    args(raw, ..)
  {
    Tuple2<ByteString, TraceContext> extracted = RemoteTrace.extractTraceContext(raw);
    Tuple2<Option<Ack>, Option<Message>> result = proceed(extracted._1());
    if (result._2().isDefined()) {
      SerializedMessage serializedMessage = result._2().get().serializedMessage();
      serializedMessage.echo$trace(extracted._2());
    }
    return result;
  }

  // ----------------------------------------------------
  // Remote message processing tracing
  // ----------------------------------------------------

  Object around(InternalActorRef recipient, Address recipientAddress, SerializedMessage serializedMessage, Option<ActorRef> senderOption):
    execution(* akka.remote.InboundMessageDispatcher+.dispatch(..)) &&
    args(recipient, recipientAddress, serializedMessage, senderOption)
  {
    ActorRef actorRef = (ActorRef) recipient;
    ActorSystemTracer tracer = actorRef.echo$tracer();

    if (disabled(tracer) || !actorRef.echo$traceable()) return proceed(recipient, recipientAddress, serializedMessage, senderOption);

    TraceContext context = serializedMessage.echo$trace();

    if (Uuid.isZero(context.trace())) return proceed(recipient, recipientAddress, serializedMessage, senderOption);

    ActorInfo info = actorRef.echo$info();
    Object message = ""; // no nice way to get to the deserialized message

    tracer.trace().local().start(context);
    tracer.remote().received(info, message, serializedMessage.getSerializedSize());
    Object result = proceed(recipient, recipientAddress, serializedMessage, senderOption);
    tracer.remote().completed(info, message);
    tracer.trace().local().end();
    return result;
  }

  // ----------------------------------------------------
  // Tracer attached to remote event publisher
  // ----------------------------------------------------

  private volatile ActorSystemTracer EventPublisher._echo$tracer;

  private ActorSystemTracer EventPublisher.echo$tracer() {
    return _echo$tracer;
  }

  private void EventPublisher.echo$tracer(ActorSystemTracer tracer) {
    _echo$tracer = tracer;
  }

  // attach the tracer to remote transport

  before(EventPublisher publisher, ActorSystem system):
    execution(akka.remote.EventPublisher.new(..)) &&
    this(publisher) &&
    args(system, ..)
  {
    publisher.echo$tracer((ActorSystemTracer) system.tracer());
  }

  // ----------------------------------------------------
  // Remote life-cycle event tracing
  // ----------------------------------------------------

  before(EventPublisher publisher, RemotingLifecycleEvent event):
    execution(* akka.remote.EventPublisher.notifyListeners(..)) &&
    this(publisher) &&
    args(event, ..)
  {
    ActorSystemTracer tracer = publisher.echo$tracer();
    if (enabled(tracer)) {
      tracer.remote().lifecycle(event);
    }
  }

}
