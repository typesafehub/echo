/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */
package com.typesafe.atmos.trace

import akka.actor._
import akka.dispatch.Dispatcher
import akka.dispatch.ExecutorServiceDelegate
import akka.dispatch.MessageDispatcher
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.{ ThreadPoolExecutor, TimeUnit }
import scala.collection.mutable.{ Set ⇒ MutableSet }
import scala.concurrent.duration.Duration
import scala.concurrent.forkjoin.ForkJoinPool

object DispatcherMonitor {

  case class DispatcherStart(dispatcher: MessageDispatcher)

  case class DispatcherShutdown(dispatcher: MessageDispatcher)

  case object Tick

}

class DispatcherMonitor(trace: Trace) extends Actor with ActorLogging {

  import DispatcherMonitor._

  val dispatchers = MutableSet[MessageDispatcher]()

  val pollInterval = Duration(trace.settings.dispatcherPollInterval, TimeUnit.MILLISECONDS)

  val scheduled: Cancellable =
    context.system.scheduler.schedule(pollInterval, pollInterval, self, Tick)(context.system.dispatcher)

  override def postStop() {
    scheduled.cancel()
  }

  def receive = {
    case DispatcherStart(dispatcher) ⇒
      dispatchers += dispatcher
    case DispatcherShutdown(dispatcher) ⇒
      dispatchers -= dispatcher
    case Tick ⇒
      createDispatcherStats
  }

  def createDispatcherStats() = {
    def sendThreadPoolTrace(dispatcher: String, dispatcherType: String, threadPoolExecutor: ThreadPoolExecutor) = {
      trace.event(
        DispatcherStatus(
          dispatcher,
          dispatcherType + "-TP",
          DispatcherMetrics(
            threadPoolExecutor.getCorePoolSize,
            threadPoolExecutor.getMaximumPoolSize,
            threadPoolExecutor.getKeepAliveTime(TimeUnit.MILLISECONDS),
            threadPoolExecutor.getRejectedExecutionHandler.getClass.getSimpleName,
            threadPoolExecutor.getActiveCount,
            threadPoolExecutor.getTaskCount,
            threadPoolExecutor.getCompletedTaskCount,
            threadPoolExecutor.getLargestPoolSize,
            threadPoolExecutor.getPoolSize,
            threadPoolExecutor.getQueue.size)))
    }

    def sendForkJoinTrace(dispatcher: String, dispatcherType: String, forkJoinPool: ForkJoinPool) = {
      trace.event(
        DispatcherStatus(
          dispatcher,
          dispatcherType + "-FJ",
          DispatcherMetrics(
            corePoolSize = forkJoinPool.getParallelism,
            maximumPoolSize = forkJoinPool.getParallelism,
            keepAliveTime = -1L,
            rejectedHandler = "",
            activeThreadCount = forkJoinPool.getActiveThreadCount,
            taskCount = -1L,
            completedTaskCount = -1L,
            largestPoolSize = forkJoinPool.getParallelism,
            poolSize = forkJoinPool.getPoolSize,
            queueSize = (forkJoinPool.getQueuedTaskCount + forkJoinPool.getQueuedSubmissionCount))))
    }

    for (dispatcher ← dispatchers) {
      dispatcher match {
        case x: Dispatcher ⇒
          // executorService is protected
          val method = classOf[Dispatcher].getDeclaredMethod("executorService")
          method.setAccessible(true)
          val executorService = method.invoke(x) match {
            case y: ExecutorServiceDelegate ⇒ y.executor
            case other                      ⇒ other
          }

          executorService match {
            case fjp: ForkJoinPool ⇒
              sendForkJoinTrace(dispatcher.id, dispatcher.getClass.getSimpleName, fjp)
            case tpe: ThreadPoolExecutor ⇒
              sendThreadPoolTrace(dispatcher.id, dispatcher.getClass.getSimpleName, tpe)
            case _ ⇒
          }
        case _ ⇒ // Right now we only handle Dispatcher (BalancingDispatcher is subclass)
      }
    }
  }

}
