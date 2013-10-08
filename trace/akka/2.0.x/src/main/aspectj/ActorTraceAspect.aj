/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */

package com.typesafe.trace;

import akka.actor.ActorCell;
import akka.actor.ActorPath;
import akka.actor.ActorRef;
import akka.actor.ActorRefProvider;
import akka.actor.ActorSystem;
import akka.actor.ActorSystemImpl;
import akka.actor.Deploy;
import akka.actor.Failed;
import akka.actor.InternalActorRef;
import akka.actor.LocalActorRef;
import akka.actor.LocalActorRefProvider;
import akka.actor.MinimalActorRef;
import akka.actor.Props;
import akka.actor.ScalaActorRef;
import akka.actor.Scheduler;
import akka.dispatch.Await.CanAwait;
import akka.dispatch.Dispatchers;
import akka.dispatch.Envelope;
import akka.dispatch.ExecutionContext;
import akka.dispatch.Future;
import akka.dispatch.Mailbox;
import akka.dispatch.MessageDispatcher;
import akka.dispatch.SystemMessage;
import akka.event.EventStream;
import akka.event.SubchannelClassification;
import akka.pattern.AskSupport;
import akka.pattern.PromiseActorRef;
import akka.util.Duration;
import akka.util.Timeout;
import com.typesafe.trace.util.Uuid;
import com.typesafe.config.Config;
import java.util.concurrent.TimeoutException;
import org.jboss.netty.akka.util.TimerTask;
import scala.Either;
import scala.Function0;
import scala.Function1;
import scala.Option;

privileged aspect ActorTraceAspect {

  // ----------------------------------------------------
  // Tracer attached to actor system
  // ----------------------------------------------------

  declare parents: ActorSystem implements WithTracer;

  private volatile ActorSystemTracer ActorSystem._echo$tracer;

  private ActorSystemTracer ActorSystem.echo$tracer() {
    return _echo$tracer;
  }

  private void ActorSystem.echo$tracer(ActorSystemTracer tracer) {
    _echo$tracer = tracer;
  }

  public Tracer ActorSystem.tracer() {
    return (Tracer) _echo$tracer;
  }

  public boolean enabled(ActorSystemTracer tracer) {
    return tracer != null && tracer.enabled();
  }

  public boolean disabled(ActorSystemTracer tracer) {
    return tracer == null || !tracer.enabled();
  }

  // attach new tracer to system

  before(ActorSystemImpl system, String name, Config config, ClassLoader classLoader):
    execution(akka.actor.ActorSystemImpl.new(..)) &&
    this(system) &&
    args(name, config, classLoader)
  {
    ActorSystemTracer tracer = ActorSystemTracer.create(name, config, classLoader);
    system.echo$tracer(tracer);
    if (enabled(tracer)) {
      tracer.actor().systemStarted(System.currentTimeMillis());
    }
  }

  // system start - wrap in an empty trace context

  Object around(ActorSystemImpl system):
    execution(* akka.actor.ActorSystemImpl.start(..)) &&
    this(system)
  {
    ActorSystemTracer tracer = system.echo$tracer();
    if (disabled(tracer)) return proceed(system);
    tracer.trace().local().start(TraceContext.EmptyTrace());
    Object result = proceed(system);
    tracer.trace().local().end();
    return result;
  }

  // system shutdown

  after(ActorSystemImpl system):
    execution(* akka.actor.ActorSystemImpl.shutdown(..)) &&
    this(system)
  {
    ActorSystemTracer tracer = system.echo$tracer();
    if (enabled(tracer)) {
      tracer.shutdown(system);
    }
  }

  // ----------------------------------------------------
  // Tracer attached to dispatchers
  // ----------------------------------------------------

  private volatile ActorSystemTracer Dispatchers._echo$tracer;

  private ActorSystemTracer Dispatchers.echo$tracer() {
    return _echo$tracer;
  }

  private void Dispatchers.echo$tracer(ActorSystemTracer tracer) {
    _echo$tracer = tracer;
  }

  private volatile ActorSystemTracer MessageDispatcher._echo$tracer;

  private ActorSystemTracer MessageDispatcher.echo$tracer() {
    return _echo$tracer;
  }

  private void MessageDispatcher.echo$tracer(ActorSystemTracer tracer) {
    _echo$tracer = tracer;
  }

  before(Dispatchers dispatchers, ActorSystemImpl system):
    execution(akka.dispatch.Dispatchers.new(..)) &&
    this(dispatchers) &&
    cflow(execution(akka.actor.ActorSystemImpl.new(..)) && this(system))
  {
    dispatchers.echo$tracer(system.echo$tracer());
  }

  after(Dispatchers dispatchers) returning(MessageDispatcher dispatcher):
    execution(* akka.dispatch.Dispatchers.lookup(..)) &&
    this(dispatchers)
  {
    dispatcher.echo$tracer(dispatchers.echo$tracer());
  }

  // ----------------------------------------------------
  // Dispatcher startup and shutdown
  // ----------------------------------------------------

  after(MessageDispatcher dispatcher):
    call(* akka.dispatch.ExecutorServiceFactory.createExecutorService(..)) &&
    cflow((execution(* akka.dispatch.MessageDispatcher.registerForExecution(..)) ||
           execution(* akka.dispatch.MessageDispatcher.executeTask(..))) &&
           this(dispatcher))
  {
    ActorSystemTracer tracer = dispatcher.echo$tracer();
    if (enabled(tracer)) {
      tracer.dispatcher().started(dispatcher);
    }
  }

  after(MessageDispatcher dispatcher):
    execution(* akka.dispatch.MessageDispatcher.shutdown(..)) &&
    this(dispatcher)
  {
    ActorSystemTracer tracer = dispatcher.echo$tracer();
    if (enabled(tracer)) {
      tracer.dispatcher().shutdown(dispatcher);
    }
  }

  // ----------------------------------------------------
  // Tracer attached to mailbox
  // ----------------------------------------------------

  private volatile ActorSystemTracer Mailbox._echo$tracer;

  private ActorSystemTracer Mailbox.echo$tracer() {
    return _echo$tracer;
  }

  private void Mailbox.echo$tracer(ActorSystemTracer tracer) {
    _echo$tracer = tracer;
  }

  after(MessageDispatcher dispatcher) returning(Mailbox mailbox):
    execution(* akka.dispatch.MessageDispatcher+.createMailbox(..)) &&
    this(dispatcher)
  {
    mailbox.echo$tracer(dispatcher.echo$tracer());
  }

  // ----------------------------------------------------
  // Tracer attached to actor ref provider
  // ----------------------------------------------------

  declare parents: LocalActorRefProvider implements WithTracer;

  private volatile ActorSystemTracer LocalActorRefProvider._echo$tracer;

  private ActorSystemTracer LocalActorRefProvider.echo$tracer() {
    return _echo$tracer;
  }

  private void LocalActorRefProvider.echo$tracer(ActorSystemTracer tracer) {
    _echo$tracer = tracer;
  }

  public Tracer LocalActorRefProvider.tracer() {
    return (Tracer) _echo$tracer;
  }

  // attach the tracer to local actor ref provider

  before(LocalActorRefProvider provider, ActorSystemImpl system):
    execution(* akka.actor.LocalActorRefProvider.init(..)) &&
    this(provider) &&
    args(system)
  {
    provider.echo$tracer(system.echo$tracer());
  }

  // ----------------------------------------------------
  // Actor ref tracing metadata
  // ----------------------------------------------------

  declare parents: ActorRef implements TraceInfo;

  // tracer

  private volatile ActorSystemTracer ActorRef._echo$tracer;

  public ActorSystemTracer ActorRef.echo$tracer() {
    return _echo$tracer;
  }

  public void ActorRef.echo$tracer(ActorSystemTracer tracer) {
    _echo$tracer = tracer;
  }

  // identifier

  private volatile String ActorRef._echo$identifier;

  public String ActorRef.echo$identifier() {
    return _echo$identifier;
  }

  public void ActorRef.echo$identifier(String identifier) {
    _echo$identifier = identifier;
  }

  // traceable

  private volatile boolean ActorRef._echo$traceable = false;

  public boolean ActorRef.echo$traceable() {
    return _echo$traceable;
  }

  public void ActorRef.echo$traceable(boolean traceable) {
    _echo$traceable = traceable;
  }

  // actor info

  private volatile ActorInfo ActorRef._echo$info;

  public ActorInfo ActorRef.echo$info() {
    return _echo$info;
  }

  public void ActorRef.echo$info(ActorInfo info) {
    _echo$info = info;
  }

  public Info ActorRef.info() {
    return (Info) this._echo$info;
  }

  // ----------------------------------------------------
  // Actor creation tracing
  // ----------------------------------------------------

  // top-level requested and created events

  ActorRef around(ActorSystemImpl system, Props props, String name):
    execution(* akka.actor.ActorSystemImpl.systemActorOf(..)) &&
    this(system) &&
    args(props, name)
  {
    ActorSystemTracer tracer = system.echo$tracer();
    ActorRef guardian = system.systemGuardian();

    if (disabled(tracer) || name == null) return proceed(system, props, name);

    String requestedIdentifier = tracer.actor().identifier(guardian.path().child(name));
    boolean requestedTraceable = tracer.actor().traceable(requestedIdentifier);

    TraceContext context;
    if (guardian.echo$traceable() && requestedTraceable) {
      context = tracer.actor().requestedTopLevelActor(guardian.echo$identifier(), guardian.echo$info(), name);
    } else {
      context = TraceContext.NoTrace();
    }

    tracer.trace().local().start(context);
    ActorRef actorRef = proceed(system, props, name);
    if (requestedTraceable) tracer.actor().createdTopLevelActor(actorRef.echo$info());
    tracer.trace().local().end();
    return actorRef;
  }

  ActorRef around(ActorSystemImpl system, Props props, String name):
    execution(* akka.actor.ActorSystemImpl.actorOf(..)) &&
    this(system) &&
    args(props, name)
  {
    ActorSystemTracer tracer = system.echo$tracer();
    ActorRef guardian = system.guardian();

    if (disabled(tracer) || name == null) return proceed(system, props, name);

    String requestedIdentifier = tracer.actor().identifier(guardian.path().child(name));
    boolean requestedTraceable = tracer.actor().traceable(requestedIdentifier);

    TraceContext context;
    if (guardian.echo$traceable() && requestedTraceable) {
      context = tracer.actor().requestedTopLevelActor(guardian.echo$identifier(), guardian.echo$info(), name);
    } else {
      context = TraceContext.NoTrace();
    }

    tracer.trace().local().start(context);
    ActorRef actorRef = proceed(system, props, name);
    if (requestedTraceable) tracer.actor().createdTopLevelActor(actorRef.echo$info());
    tracer.trace().local().end();
    return actorRef;
  }

  ActorRef around(ActorSystemImpl system, Props props):
    execution(* akka.actor.ActorSystemImpl.actorOf(..)) &&
    this(system) &&
    args(props)
  {
    ActorSystemTracer tracer = system.echo$tracer();
    ActorRef guardian = system.guardian();

    if (disabled(tracer)) return proceed(system, props);

    String name = Traceable.RandomPlaceholder();
    String requestedIdentifier = tracer.actor().identifier(guardian.path().child(name));
    boolean requestedTraceable = tracer.actor().traceable(requestedIdentifier);

    TraceContext context;
    if (guardian.echo$traceable() && requestedTraceable) {
      context = tracer.actor().requestedTopLevelActor(guardian.echo$identifier(), guardian.echo$info(), name);
    } else {
      context = TraceContext.NoTrace();
    }

    tracer.trace().local().start(context);
    ActorRef actorRef = proceed(system, props);
    if (requestedTraceable) tracer.actor().createdTopLevelActor(actorRef.echo$info());
    tracer.trace().local().end();
    return actorRef;
  }

  // actor requested and created events

  InternalActorRef around(ActorRefProvider provider, ActorSystemImpl system, Props props, InternalActorRef supervisor,
    ActorPath path, boolean systemService, Option<Deploy> deploy, boolean lookupDeploy):
    execution(* akka.actor.LocalActorRefProvider.actorOf(..)) &&
    this(provider) &&
    args(system, props, supervisor, path, systemService, deploy, lookupDeploy)
  {
    ActorSystemTracer tracer = system.echo$tracer();

    if (disabled(tracer) || !supervisor.echo$traceable()) return proceed(provider, system, props, supervisor, path, systemService, deploy, lookupDeploy);

    String requestedIdentifier = tracer.actor().identifier(path);
    boolean requestedTraceable = tracer.actor().traceable(requestedIdentifier);
    if (!requestedTraceable) return proceed(provider, system, props, supervisor, path, systemService, deploy, lookupDeploy);

    ActorInfo info = tracer.actor().info(path, props.dispatcher(), false);
    TraceContext context = tracer.actor().requested(supervisor.echo$info(), info);
    tracer.trace().local().start(context);
    InternalActorRef child = proceed(provider, system, props, supervisor, path, systemService, deploy, lookupDeploy);
    tracer.actor().created(child.echo$info());
    tracer.trace().local().end();
    return child;
  }

  // attach metadata to newly created actor

  before(LocalActorRef actorRef, ActorSystemImpl system, Props props,
         InternalActorRef supervisor, ActorPath path, boolean systemService):
    execution(akka.actor.LocalActorRef.new(..)) &&
    this(actorRef) &&
    args(system, props, supervisor, path, systemService, ..)
  {
    ActorSystemTracer tracer = system.echo$tracer();
    actorRef.echo$tracer(tracer);
    if (enabled(tracer)) {
      String identifier = tracer.actor().identifier(path);
      actorRef.echo$identifier(identifier);
      boolean traceable = tracer.actor().traceable(identifier);
      actorRef.echo$traceable(traceable);
      if (traceable) {
        ActorInfo info = tracer.actor().info(path, props.dispatcher(), false);
        actorRef.echo$info(info);
      }
    }
  }

  // ----------------------------------------------------
  // Trace context transfer with system message
  // ----------------------------------------------------

  private volatile TraceContext SystemMessage._echo$trace;

  public TraceContext SystemMessage.echo$trace() {
    return _echo$trace;
  }

  public void SystemMessage.echo$trace(TraceContext context) {
    _echo$trace = context;
  }

  // ----------------------------------------------------
  // Actor system message send tracing
  // ----------------------------------------------------

  before(Mailbox mailbox, ActorRef actorRef, SystemMessage message):
    execution(* akka.dispatch.Mailbox+.systemEnqueue(..)) &&
    this(mailbox) &&
    args(actorRef, message)
  {
    ActorSystemTracer tracer = actorRef.echo$tracer();
    if (enabled(tracer) && actorRef.echo$traceable()) {
      TraceContext context = tracer.actor().sysMsgDispatched(actorRef.echo$info(), message);
      message.echo$trace(context);
    }
  }

  // ----------------------------------------------------
  // Actor system message processing tracing
  // ----------------------------------------------------

  Object around(ActorCell actorCell, SystemMessage message):
    execution(* akka.actor.ActorCell.systemInvoke(..)) &&
    this(actorCell) &&
    args(message)
  {
    ActorRef actorRef = (ActorRef) actorCell.self();
    ActorSystemTracer tracer = actorRef.echo$tracer();
    TraceContext context = message.echo$trace();

    if (disabled(tracer) || !actorRef.echo$traceable() || (context == null))
      return proceed(actorCell, message);

    ActorInfo info = actorRef.echo$info();

    // set the trace context from the system message
    tracer.trace().local().start(context);
    tracer.actor().sysMsgReceived(info, message);
    Object result = proceed(actorCell, message);
    tracer.actor().sysMsgCompleted(info, message);
    tracer.trace().local().end();
    return result;
  }

  // ----------------------------------------------------
  // Transfer trace context with envelope
  // ----------------------------------------------------

  private volatile TraceContext Envelope._echo$trace = TraceContext.ZeroTrace();

  private TraceContext Envelope.echo$trace() {
    return _echo$trace;
  }

  private void Envelope.echo$trace(TraceContext context) {
    _echo$trace = context;
  }

  // ----------------------------------------------------
  // Actor message send tracing
  // ----------------------------------------------------

  after(ActorRef actorRef, Envelope envelope, Object message, ActorRef sender, ActorSystem system):
    execution(akka.dispatch.Envelope.new(..)) &&
    this(envelope) &&
    args(message, sender, system) &&
    cflow(execution(* akka.actor.LocalActorRef.$bang(..)) && this(actorRef))
  {
    ActorSystemTracer tracer = actorRef.echo$tracer();
    if (enabled(tracer)) {
      if (message instanceof Failed) {
        if (sender != null && sender.echo$traceable()) {
          TraceContext context = tracer.actor().failed(sender.echo$info(), ((Failed) message).cause(), actorRef.echo$info());
          envelope.echo$trace(context);
        }
      } else if (actorRef.echo$traceable()) {
        ActorInfo senderInfo = (sender != null && sender.echo$traceable()) ? sender.echo$info() : null;
        TraceContext context = tracer.actor().told(actorRef.echo$identifier(), actorRef.echo$info(), message, senderInfo);
        envelope.echo$trace(context);
      }
    }
  }

  // ----------------------------------------------------
  // Actor message processing tracing
  // ----------------------------------------------------

  Object around(ActorCell actorCell, Envelope envelope):
    execution(* akka.actor.ActorCell.invoke(..)) &&
    this(actorCell) &&
    args(envelope)
  {
    ActorRef actorRef = (ActorRef) actorCell.self();
    ActorSystemTracer tracer = actorRef.echo$tracer();
    TraceContext context = envelope.echo$trace();

    if (disabled(tracer) || !actorRef.echo$traceable() || (context == null))
      return proceed(actorCell, envelope);

    ActorInfo info = actorRef.echo$info();
    Object message = envelope.message();

    tracer.trace().local().start(context);
    tracer.actor().received(info, message);
    Object result = proceed(actorCell, envelope);
    tracer.actor().completed(info, message);
    tracer.trace().local().end();
    return result;
  }

  // ----------------------------------------------------
  // Ask pattern tracing
  // ----------------------------------------------------

  // attach actor ref metadata to promise actor ref

  before(PromiseActorRef actorRef, ActorRefProvider provider):
    execution(akka.pattern.PromiseActorRef.new(..)) &&
    this(actorRef) &&
    args(provider, ..)
  {
    if (provider instanceof WithTracer) {
      ActorSystemTracer tracer = (ActorSystemTracer) ((WithTracer) provider).tracer();
      actorRef.echo$tracer(tracer);
      if (enabled(tracer)) {
        ActorPath path = actorRef.path();
        String identifier = tracer.actor().identifier(path);
        actorRef.echo$identifier(identifier);
        boolean traceable = tracer.trace().active();
        actorRef.echo$traceable(traceable);
        if (traceable) {
          ActorInfo info = tracer.actor().info(path, null, false);
          actorRef.echo$info(info);
        }
      }
    }
  }

  // promise actor created

  after(PromiseActorRef actorRef):
    execution(akka.pattern.PromiseActorRef.new(..)) &&
    this(actorRef)
  {
    ActorSystemTracer tracer = actorRef.echo$tracer();
    if (enabled(tracer) && actorRef.echo$traceable()) {
      tracer.actor().tempCreated(actorRef.echo$info());
    }
  }

  // wrap pattern.ask with a branched asked event

  Future<Object> around(AskSupport askSupport, ActorRef actorRef, Object message, Timeout timeout):
    execution(* akka.pattern.AskSupport$class.ask(..)) &&
    args(askSupport, actorRef, message, timeout)
  {
    ActorSystemTracer tracer = actorRef.echo$tracer();

    if (disabled(tracer)) return proceed(askSupport, actorRef, message, timeout);

    TraceContext context = TraceContext.EmptyTrace();

    if (actorRef.echo$traceable()) {
      context = tracer.actor().asked(actorRef.echo$identifier(), actorRef.echo$info(), message);
    }

    tracer.trace().local().start(context);
    Future<Object> future = proceed(askSupport, actorRef, message, timeout);
    tracer.trace().local().end();
    return future;
  }

  // promise actor ref message processing

  Object around(PromiseActorRef actorRef, Object message, ActorRef sender):
    execution(* akka.pattern.PromiseActorRef.$bang(..)) &&
    this(actorRef) &&
    args(message, sender)
  {
    ActorSystemTracer tracer = actorRef.echo$tracer();

    if (disabled(tracer) || !actorRef.echo$traceable()) return proceed(actorRef, message, sender);

    ActorInfo info = actorRef.echo$info();
    ActorInfo senderInfo = (sender != null && sender.echo$traceable()) ? sender.echo$info() : null;
    TraceContext context = tracer.actor().tempTold(actorRef.echo$identifier(), info, message, senderInfo);
    tracer.trace().local().start(context);
    tracer.actor().tempReceived(info, message);
    Object result = proceed(actorRef, message, sender);
    tracer.actor().tempCompleted(info, message);
    tracer.trace().local().end();
    return result;
  }

  // promise actor ref stop

  after(PromiseActorRef actorRef):
    execution(* akka.pattern.PromiseActorRef.stop(..)) &&
    this(actorRef)
  {
    ActorSystemTracer tracer = actorRef.echo$tracer();
    if (enabled(tracer) && actorRef.echo$traceable()) {
      tracer.actor().tempStopped(actorRef.echo$info());
    }
  }

  // ----------------------------------------------------
  // Future tracing
  // ----------------------------------------------------

  // add trace metadata to future

  declare parents: Future implements TraceInfo;

  // tracer

  private volatile ActorSystemTracer Future._echo$tracer;

  public ActorSystemTracer Future.echo$tracer() {
    return _echo$tracer;
  }

  public void Future.echo$tracer(ActorSystemTracer tracer) {
    _echo$tracer = tracer;
  }

  // sampled

  private volatile int Future._echo$sampled;

  public int Future.echo$sampled() {
    return _echo$sampled;
  }

  public void Future.echo$sampled(int sampled) {
    _echo$sampled = sampled;
  }

  public boolean Future.echo$traceable() {
    return _echo$sampled > 0;
  }

  // info

  private volatile FutureInfo Future._echo$info;

  public FutureInfo Future.echo$info() {
    if (_echo$info == null) return FutureTrace.ZeroFutureInfo();
    else return _echo$info;
  }

  public void Future.echo$info(FutureInfo info) {
    _echo$info = info;
  }

  public Info Future.info() {
    return (Info) this._echo$info;
  }

  // attach trace metadata to future

  after(Future future, ExecutionContext executor):
    execution(akka.dispatch.Future+.new(..)) &&
    this(future) &&
    args(executor)
  {
    if (executor instanceof MessageDispatcher) {
      MessageDispatcher dispatcher = (MessageDispatcher) executor;
      ActorSystemTracer tracer = dispatcher.echo$tracer();
      future.echo$tracer(tracer);
      if (enabled(tracer)) {
        int sampled = -1; // default to empty trace
        if (tracer.trace().within())
          sampled = tracer.trace().sampled();
        else if (tracer.trace().settings().zeroContextFutures())
          sampled = 1;
        future.echo$sampled(sampled);
        if (sampled > 0) {
          FutureInfo info = tracer.future().newInfo(dispatcher.id());
          future.echo$info(info);
          tracer.future().created(info);
        }
      }
    } else {
      future.echo$tracer(ActorSystemTracer.disabled());
    }
  }

  after(Future future, Either value, ExecutionContext executor):
    execution(akka.dispatch.Future+.new(..)) &&
    this(future) &&
    args(value, executor)
  {
    if (executor instanceof MessageDispatcher) {
      MessageDispatcher dispatcher = (MessageDispatcher) executor;
      ActorSystemTracer tracer = dispatcher.echo$tracer();
      future.echo$tracer(tracer);
      if (enabled(tracer)) {
        int sampled = -1; // default to empty trace
        if (tracer.trace().within())
          sampled = tracer.trace().sampled();
        else if (tracer.trace().settings().zeroContextFutures())
          sampled = 1;
        future.echo$sampled(sampled);
        if (sampled > 0) {
          FutureInfo info = tracer.future().newInfo(dispatcher.id());
          future.echo$info(info);
          tracer.future().created(info);
          tracer.future().completed(info, value);
        }
      }
    } else {
      future.echo$tracer(ActorSystemTracer.disabled());
    }
  }

  // future apply

  after(Future future, ExecutionContext executor):
    execution(akka.dispatch.Future+.new(..)) &&
    this(future) &&
    args(executor) &&
    cflow(execution(* akka.dispatch.Future$.apply(..)))
  {
    if (executor instanceof MessageDispatcher && future.echo$tracer().enabled()) {
      FutureTrace.store(future.echo$sampled(), future.echo$info());
    }
  }

  Object around(ExecutionContext executor, Runnable runnable):
    execution(* akka.dispatch.ExecutionContext+.execute(..)) &&
    this(executor) &&
    args(runnable) &&
    cflow(execution(* akka.dispatch.Future$.apply(..)))
  {
    int sampled = FutureTrace.sampled();
    FutureInfo futureInfo = FutureTrace.info();
    FutureTrace.clear();
    if (executor instanceof MessageDispatcher) {
      MessageDispatcher dispatcher = (MessageDispatcher) executor;
      ActorSystemTracer tracer = dispatcher.echo$tracer();
      if (disabled(tracer)) return proceed(executor, runnable);
      TaskInfo taskInfo = tracer.dispatcher().newTaskInfo(dispatcher.id());
      TraceContext context = TraceContext.EmptyTrace();
      if (sampled == 0) {
        context = TraceContext.NoTrace();
      } else if (sampled > 0) {
        context = tracer.future().scheduled(futureInfo, taskInfo);
      }
      TracedRunnable tracedRunnable = new TracedRunnable(runnable, tracer, context, taskInfo);
      return proceed(executor, tracedRunnable);
    } else {
      return proceed(executor, runnable);
    }
  }

  // future completion

  before(Future future, Either value):
    execution(* akka.dispatch.Promise+.tryComplete(..)) &&
    this(future) &&
    args(value)
  {
    ActorSystemTracer tracer = future.echo$tracer();
    if (enabled(tracer) && future.echo$traceable()) {
      tracer.future().completed(future.echo$info(), value);
    }
  }

  // trace future callbacks - add metadata to wrapped function

  Object around(Future future, Function1 func):
    execution(* akka.dispatch.Future+.onComplete(..)) &&
    this(future) &&
    args(func)
  {
    ActorSystemTracer tracer = future.echo$tracer();
    if (disabled(tracer)) return proceed(future, func);
    int sampled = future.echo$sampled();
    FutureInfo info = future.echo$info();
    TraceContext context = TraceContext.EmptyTrace();
    if (sampled == 0) {
        context = TraceContext.NoTrace();
    } else if (sampled > 0) {
      context = tracer.future().callbackAdded(info);
    }
    FutureCallback callback = new FutureCallback(func, tracer, context, info);
    return proceed(future, callback);
  }

  Object around(Future future, Duration duration, CanAwait permit):
    execution(* akka.dispatch.Future+.ready(..)) &&
    this(future) &&
    args(duration, permit)
  {
    ActorSystemTracer tracer = future.echo$tracer();
    if (disabled(tracer) || !future.echo$traceable()) return proceed(future, duration, permit);
    try {
      Object result = proceed(future, duration, permit);
      tracer.future().awaited(future.echo$info());
      return result;
    } catch (TimeoutException exception) {
      // exception is rethrown in timedOut
      tracer.future().timedOut(future.echo$info(), duration, exception);
      return null;
    }
  }

  // ----------------------------------------------------
  // Scheduler tracing
  // ----------------------------------------------------

  // attach tracer to scheduler

  private volatile ActorSystemTracer Scheduler._echo$tracer;

  private ActorSystemTracer Scheduler.echo$tracer() {
    return _echo$tracer;
  }

  private void Scheduler.echo$tracer(ActorSystemTracer tracer) {
    _echo$tracer = tracer;
  }

  after(ActorSystemImpl system) returning(Scheduler scheduler):
    execution(* akka.actor.ActorSystemImpl.createScheduler(..)) &&
    this(system)
  {
    scheduler.echo$tracer(system.echo$tracer());
  }

  // attach tracer and info to scheduler timeout

  private volatile ActorSystemTracer org.jboss.netty.akka.util.Timeout._echo$tracer;

  public ActorSystemTracer org.jboss.netty.akka.util.Timeout.echo$tracer() {
    return _echo$tracer;
  }

  public void org.jboss.netty.akka.util.Timeout.echo$tracer(ActorSystemTracer tracer) {
    _echo$tracer = tracer;
  }

  private volatile TaskInfo org.jboss.netty.akka.util.Timeout._echo$info;

  private TaskInfo org.jboss.netty.akka.util.Timeout.echo$info() {
    return _echo$info;
  }

  private void org.jboss.netty.akka.util.Timeout.echo$info(TaskInfo info) {
    _echo$info = info;
  }

  // schedule once tracing

  org.jboss.netty.akka.util.Timeout around(Scheduler scheduler, TimerTask task, Duration delay):
    execution(* org.jboss.netty.akka.util.HashedWheelTimer.newTimeout(..)) &&
    args(task, delay) &&
    cflow(execution(* akka.actor.Scheduler+.scheduleOnce(..)) && this(scheduler))
  {
    ActorSystemTracer tracer = scheduler.echo$tracer();
    if (disabled(tracer)) return proceed(scheduler, task, delay);
    TaskInfo info = tracer.scheduler().newInfo();
    TraceContext context = tracer.scheduler().scheduledOnce(info, delay);
    TracedTimerTask tracedTask = new TracedTimerTask(task, tracer, context, info);
    org.jboss.netty.akka.util.Timeout timeout = proceed(scheduler, tracedTask, delay);
    timeout.echo$tracer(scheduler.echo$tracer());
    timeout.echo$info(info);
    return timeout;
  }

  // runnable scheduled on dispatcher tracing

  Object around(MessageDispatcher dispatcher, Runnable runnable):
    execution(* akka.dispatch.MessageDispatcher.execute(..)) &&
    this(dispatcher) &&
    args(runnable) &&
    cflow(execution(* org.jboss.netty.akka.util.TimerTask.run(..)))
  {
    ActorSystemTracer tracer = dispatcher.echo$tracer();
    if (disabled(tracer)) return proceed(dispatcher, runnable);
    TaskInfo info = tracer.dispatcher().newTaskInfo(dispatcher.id());
    TraceContext context = tracer.dispatcher().runnableScheduled(info);
    TracedRunnable tracedRunnable = new TracedRunnable(runnable, tracer, context, info);
    return proceed(dispatcher, tracedRunnable);
  }

  // scheduled task cancelled tracing

  before(org.jboss.netty.akka.util.Timeout timeout):
    execution(* org.jboss.netty.akka.util.Timeout.cancel(..)) &&
    this(timeout)
  {
    ActorSystemTracer tracer = timeout.echo$tracer();
    if (enabled(tracer)) {
      TaskInfo info = timeout.echo$info();
      if (info != null && !timeout.isCancelled() && !timeout.isExpired()) {
        tracer.scheduler().cancelled(info);
      }
    }
  }

  // ----------------------------------------------------
  // Event stream tracing
  // ----------------------------------------------------

  // attach tracer to event stream

  private volatile ActorSystemTracer EventStream._echo$tracer;

  private ActorSystemTracer EventStream.echo$tracer() {
    return _echo$tracer;
  }

  private void EventStream.echo$tracer(ActorSystemTracer tracer) {
    _echo$tracer = tracer;
  }

  before(ActorSystemImpl system, EventStream eventStream):
    execution(akka.event.EventStream.new(..)) &&
    this(eventStream) &&
    cflow(execution(akka.actor.ActorSystemImpl.new(..)) && this(system))
  {
    eventStream.echo$tracer(system.echo$tracer());
  }

  // event stream publish tracing

  after(SubchannelClassification eventBus, Object event):
    execution(* akka.event.SubchannelClassification$class.publish(..)) &&
    args(eventBus, event)
  {
    if (eventBus instanceof EventStream) {
      EventStream eventStream = (EventStream) eventBus;
      ActorSystemTracer tracer = eventStream.echo$tracer();
      if (enabled(tracer)) {
        tracer.eventStream().published(event);
      }
    }
  }

  // ----------------------------------------------------
  // Empty local actor ref tracing
  // ----------------------------------------------------

  // attach metadata to newly created empty local actor refs
  // used to check traceability for event stream dead letters

  before(ActorRef actorRef, ActorRefProvider provider, ActorPath path, EventStream eventStream):
    execution(akka.actor.EmptyLocalActorRef+.new(..)) &&
    this(actorRef) &&
    args(provider, path, eventStream)
  {
    ActorSystemTracer tracer = eventStream.echo$tracer();
    actorRef.echo$tracer(tracer);
    if (enabled(tracer)) {
      String identifier = tracer.actor().identifier(path);
      actorRef.echo$identifier(identifier);
      boolean traceable = tracer.actor().traceable(identifier);
      actorRef.echo$traceable(traceable);
      if (traceable) {
        ActorInfo info = tracer.actor().info(path, "", false);
        actorRef.echo$info(info);
      }
    }
  }

}
