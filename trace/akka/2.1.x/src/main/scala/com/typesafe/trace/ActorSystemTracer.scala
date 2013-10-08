/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */

package com.typesafe.trace

import akka.actor.{ ActorRef, ActorSystem, Props }
import com.typesafe.config.Config
import java.util.concurrent.{ CountDownLatch, TimeoutException, TimeUnit }
import scala.concurrent.duration.Duration

object ActorSystemTracer {
  val disabled = new DisabledActorSystemTracer(null)

  /**
   * Get the ActorSystemTracer from a traced ActorSystem. Internal use only.
   */
  def apply(system: ActorSystem): ActorSystemTracer = Tracer(system).asInstanceOf[ActorSystemTracer]

  /**
   * Create a new ActorSystemTracer. First checks config for whether tracing is enabled.
   * If tracing is not enabled then an empty DisabledActorSystemTracer is returned.
   */
  def create(name: String, config: Config, classLoader: ClassLoader): ActorSystemTracer = {
    val settings = new Trace.Settings(name, config, classLoader)
    if (settings.enabled) new DefaultActorSystemTracer(settings)
    else new DisabledActorSystemTracer(settings)
  }
}

/**
 * Interface for ActorSystem tracer.
 */
abstract class ActorSystemTracer extends ExecutionContextTracer {
  def actor: ActorTrace
  def dispatcher: DispatcherTrace
  def scheduler: SchedulerTrace
  def remote: RemoteTrace
  def eventStream: EventStreamTrace
  def shutdown(system: ActorSystem): Unit
  def awaitShutdown(limit: Duration): Unit
  def updateConfig(config: Config): Unit = () // disabled for now
}

/**
 * Tracer implementation. Internal API.
 */
class DefaultActorSystemTracer(val settings: Trace.Settings) extends ActorSystemTracer {
  val enabled = settings.enabled

  val trace = new Trace(settings)

  val executionContext = new ExecutionContextTrace(trace)

  val future = new FutureTrace(trace)

  val actor = new ActorTrace(trace)

  val dispatcher = new DispatcherTrace(trace)

  val scheduler = new SchedulerTrace(trace)

  val remote = new RemoteTrace(trace)

  val eventStream = new EventStreamTrace(trace)

  val systemMetricsMonitor: Option[ActorRef] = {
    if (settings.useSystemMetricsMonitor)
      Some(AtmosSystem.create(Props(new SystemMetricsMonitor(trace)).withDispatcher(Trace.DispatcherId), "SystemMetricsMonitor"))
    else None
  }

  private[this] val shutdownLatch = new CountDownLatch(1)

  def shutdown(system: ActorSystem): Unit = {
    val runShutdown = new Runnable {
      def run = {
        system.awaitTermination(Duration(settings.shutdownTimeout, TimeUnit.MILLISECONDS))
        actor.systemShutdown(System.currentTimeMillis)
        trace.local.flush()
        systemMetricsMonitor foreach AtmosSystem.stop
        dispatcher.monitor foreach AtmosSystem.stop
        trace.sender.shutdown()
        shutdownLatch.countDown()
      }
    }
    val thread = new Thread(runShutdown)
    thread.setName("ActorSystemTracer-shutdown")
    thread.start()
  }

  def awaitShutdown(limit: Duration): Unit = {
    if (limit.isFinite) {
      if (!shutdownLatch.await(limit.length, limit.unit))
        throw new TimeoutException("Await shutdown of traced actor system timed out [%s]" format limit.toString)
    } else shutdownLatch.await()
  }

  override def toString = "ActorSystemTracer(%s)" format settings.systemName
}

/**
 * Disabled tracer. Enabled needs to be checked to avoid null pointer exceptions.
 */
class DisabledActorSystemTracer(val settings: Trace.Settings) extends ActorSystemTracer {
  val enabled = false
  val trace: Trace = null
  val executionContext: ExecutionContextTrace = null
  val future: FutureTrace = null
  val actor: ActorTrace = null
  val dispatcher: DispatcherTrace = null
  val scheduler: SchedulerTrace = null
  val remote: RemoteTrace = null
  val eventStream: EventStreamTrace = null
  def shutdown(system: ActorSystem): Unit = ()
  def awaitShutdown(limit: Duration): Unit = ()
  override def record(name: String, data: String): Unit = ()
  override def mark[B](name: String)(body: ⇒ B): B = body
  override def markStart(name: String): Unit = ()
  override def markEnd(name: String): Unit = ()
  override def group[B](name: String)(body: ⇒ B): B = body
  override def startGroup(name: String): Unit = ()
  override def endGroup(name: String): Unit = ()
  override def toString = "ActorSystemTracer(%s)" format settings.systemName
}
