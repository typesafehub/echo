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
import akka.actor.LocalActorRefProvider;
import akka.actor.MinimalActorRef;
import akka.actor.Props;
import akka.actor.ScalaActorRef;
import akka.actor.Scheduler;
import akka.actor.UnstartedCell;
import akka.dispatch.Dispatchers;
import akka.dispatch.Envelope;
import akka.dispatch.Mailbox;
import akka.dispatch.MessageDispatcher;
import akka.dispatch.SystemMessage;
import akka.event.EventStream;
import akka.event.SubchannelClassification;
import akka.pattern.AskSupport;
import akka.pattern.PromiseActorRef;
import akka.util.internal.TimerTask;
import akka.util.Timeout;
import com.typesafe.trace.util.Uuid;
import com.typesafe.config.Config;
import scala.concurrent.duration.Duration;
import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;
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
    ActorPath path, boolean systemService, Option<Deploy> deploy, boolean lookupDeploy, boolean async):
    execution(* akka.actor.LocalActorRefProvider.actorOf(..)) &&
    this(provider) &&
    args(system, props, supervisor, path, systemService, deploy, lookupDeploy, async)
  {
    ActorSystemTracer tracer = system.echo$tracer();

    if (disabled(tracer) || !supervisor.echo$traceable()) return proceed(provider, system, props, supervisor, path, systemService, deploy, lookupDeploy, async);

    String requestedIdentifier = tracer.actor().identifier(path);
    boolean requestedTraceable = tracer.actor().traceable(requestedIdentifier);
    if (!requestedTraceable) return proceed(provider, system, props, supervisor, path, systemService, deploy, lookupDeploy, async);

    ActorInfo info = tracer.actor().info(path, props.dispatcher(), false);
    TraceContext context = tracer.actor().requested(supervisor.echo$info(), info);
    tracer.trace().local().start(context);
    InternalActorRef child = proceed(provider, system, props, supervisor, path, systemService, deploy, lookupDeploy, async);
    tracer.actor().created(child.echo$info());
    tracer.trace().local().end();
    return child;
  }

  // attach metadata to newly created actor

  before(ActorRef actorRef, ActorSystemImpl system, Props props, InternalActorRef supervisor, ActorPath path):
    execution(akka.actor.ActorRefWithCell+.new(..)) &&
    this(actorRef) &&
    args(system, props, supervisor, path)
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
      TraceContext context = tracer.actor().message().sysMsgDispatched(actorRef.echo$info(), message);
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
    tracer.actor().message().sysMsgReceived(info, message);
    Object result = proceed(actorCell, message);
    tracer.actor().message().sysMsgCompleted(info, message);
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

  after(ActorRef actorRef, Envelope envelope, Object message, ActorRef sender):
    execution(akka.dispatch.Envelope.new(..)) &&
    this(envelope) &&
    args(message, sender) &&
    cflow(execution(* akka.actor.ActorRefWithCell+.$bang(..)) && this(actorRef))
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
  // Transfer trace context from unstarted cells
  // ----------------------------------------------------

  // a new envelope is created in UnstartedCell.replaceWith so
  // transfer the envelope trace context via the UnstartedCell

  private TraceContext UnstartedCell._echo$trace = TraceContext.ZeroTrace();

  private TraceContext UnstartedCell.echo$trace() {
    return _echo$trace;
  }

  private void UnstartedCell.echo$trace(TraceContext context) {
    _echo$trace = context;
  }

  before(Envelope envelope, UnstartedCell cell):
    execution(* akka.dispatch.Envelope.message(..)) &&
    this(envelope) &&
    cflow(execution(* akka.actor.UnstartedCell.replaceWith(..)) && this(cell))
  {
    cell.echo$trace(envelope.echo$trace());
  }

  after(Envelope envelope, UnstartedCell cell):
    execution(akka.dispatch.Envelope.new(..)) &&
    this(envelope) &&
    cflow(execution(* akka.actor.UnstartedCell.replaceWith(..)) && this(cell))
  {
    envelope.echo$trace(cell.echo$trace());
    cell.echo$trace(TraceContext.ZeroTrace());
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
    tracer.actor().message().received(info, message);
    Object result = proceed(actorCell, envelope);
    tracer.actor().message().completed(info, message);
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
    tracer.actor().message().tempReceived(info, message);
    Object result = proceed(actorRef, message, sender);
    tracer.actor().message().tempCompleted(info, message);
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

  private volatile ActorSystemTracer akka.util.internal.Timeout._echo$tracer;

  public ActorSystemTracer akka.util.internal.Timeout.echo$tracer() {
    return _echo$tracer;
  }

  public void akka.util.internal.Timeout.echo$tracer(ActorSystemTracer tracer) {
    _echo$tracer = tracer;
  }

  private volatile TaskInfo akka.util.internal.Timeout._echo$info;

  private TaskInfo akka.util.internal.Timeout.echo$info() {
    return _echo$info;
  }

  private void akka.util.internal.Timeout.echo$info(TaskInfo info) {
    _echo$info = info;
  }

  // schedule once tracing

  akka.util.internal.Timeout around(Scheduler scheduler, TimerTask task, Duration delay):
    execution(* akka.util.internal.HashedWheelTimer.newTimeout(..)) &&
    args(task, delay) &&
    cflow(execution(* akka.actor.Scheduler+.scheduleOnce(..)) && this(scheduler))
  {
    ActorSystemTracer tracer = scheduler.echo$tracer();
    if (disabled(tracer)) return proceed(scheduler, task, delay);
    TaskInfo info = tracer.scheduler().newInfo();
    TraceContext context = tracer.scheduler().scheduledOnce(info, delay);
    TracedTimerTask tracedTask = new TracedTimerTask(task, tracer, context, info);
    akka.util.internal.Timeout timeout = proceed(scheduler, tracedTask, delay);
    timeout.echo$tracer(scheduler.echo$tracer());
    timeout.echo$info(info);
    return timeout;
  }

  // runnable scheduled on dispatcher tracing

  Object around(MessageDispatcher dispatcher, Runnable runnable):
    execution(* akka.dispatch.MessageDispatcher.execute(..)) &&
    this(dispatcher) &&
    args(runnable) &&
    cflow(execution(* akka.util.internal.TimerTask.run(..)))
  {
    ActorSystemTracer tracer = dispatcher.echo$tracer();
    if (disabled(tracer)) return proceed(dispatcher, runnable);
    TaskInfo info = tracer.dispatcher().newTaskInfo(dispatcher.id());
    TraceContext context = tracer.dispatcher().runnableScheduled(info);
    TracedRunnable tracedRunnable = new TracedRunnable(runnable, tracer, context, info);
    return proceed(dispatcher, tracedRunnable);
  }

  // scheduled task cancelled tracing

  before(akka.util.internal.Timeout timeout):
    execution(* akka.util.internal.Timeout.cancel(..)) &&
    this(timeout)
  {
    ActorSystemTracer tracer = timeout.echo$tracer();
    if (enabled(tracer) && tracer.trace().sampled() > 0) {
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
