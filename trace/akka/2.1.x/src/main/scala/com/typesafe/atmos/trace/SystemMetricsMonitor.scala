/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */
package com.typesafe.atmos.trace

import akka.actor._
import akka.event.LoggingAdapter
import java.lang.management.ManagementFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration.Duration
import java.lang.reflect.InvocationTargetException

// TODO: move system metrics out of akka tracing

object SystemMetricsMonitor {
  val activeMonitorPerNode = new ConcurrentHashMap[String, SystemMetricsMonitor]
  case object SystemMetricsTick
}

class SystemMetricsMonitor(trace: Trace) extends Actor with ActorLogging {

  import SystemMetricsMonitor._

  val node = trace.settings.node

  lazy val metricsProvider: MetricsProvider = MetricsProvider(log)

  lazy val deadlockDetector = new DeadlockDetector
  val deadlockWatchFrequencyMillis = trace.settings.deadlockWatchFrequency

  val pollInterval = Duration(trace.settings.systemMetricsPollInterval, TimeUnit.MILLISECONDS)

  val scheduledSystemMetrics: Cancellable =
    context.system.scheduler.schedule(pollInterval, pollInterval, self, SystemMetricsTick)(context.system.dispatcher)

  var lastDeadlockCheck = 0L

  override def postStop(): Unit = try {
    scheduledSystemMetrics.cancel()
    metricsProvider.close()
  } finally {
    if (active()) activeMonitorPerNode.remove(node)
  }

  def receive = {
    case SystemMetricsTick ⇒
      // There is one SystemMetricsMonitor instance per traced ActorSystem.
      // Allow only one active SystemMetricsMonitor per node.
      // This has the limitation that it is not be possible to use different
      // trace event storage for the different traced ActorSystems running in
      // same jvm and configured with same node name.
      if (active()) {
        val metrics = metricsProvider.currentMetrics()
        trace.event(metrics)
        val breakpoint = lastDeadlockCheck + deadlockWatchFrequencyMillis
        val now = System.currentTimeMillis
        if (breakpoint < now) {
          lastDeadlockCheck = now
          deadlockDetector.findDeadlocks match {
            case Some(deadlocked) ⇒ trace.event(deadlocked)
            case None             ⇒
          }
        }
      }
    case _ ⇒
  }

  def active(): Boolean = {
    (activeMonitorPerNode.get(node) eq this) || (activeMonitorPerNode.putIfAbsent(node, this) eq null)
  }

}

class DeadlockDetector {

  val threadMXBean = ManagementFactory.getThreadMXBean
  var existingDeadlockedThreads = Set[Long]()

  def findDeadlocks: Option[DeadlockedThreads] = {
    val currentDeadlocks = threadMXBean.findDeadlockedThreads
    val threads = new ArrayBuffer[String]()
    val deadlocks = new ArrayBuffer[String]()

    if (currentDeadlocks != null) {
      val currentDeadlockSet = currentDeadlocks.toSet[Long]
      val newDeadlocks = currentDeadlockSet.filterNot(p ⇒ existingDeadlockedThreads.contains(p))
      newDeadlocks.foreach(d ⇒ {
        val stackTrace = new StringBuilder
        val info = threadMXBean.getThreadInfo(d, 100)
        info.getStackTrace.foreach(s ⇒ {
          stackTrace.append(" at ").append(s.toString)
        })
        threads.append(info.getThreadName)
        deadlocks.append("%s locked on %s (owned by %s): %s".format(info.getThreadName, info.getLockName, info.getLockOwnerName, stackTrace.toString))
      })

      // Keep track of what deadlocks that where found this time (used to not resend the same information)
      existingDeadlockedThreads = existingDeadlockedThreads ++ currentDeadlockSet
    }
    if (deadlocks.isEmpty) {
      None
    } else {
      val message = "Deadlocked threads: " + threads.mkString(", ")
      Some(DeadlockedThreads(message, deadlocks.toSet))
    }
  }
}

trait MetricsProvider {
  def currentMetrics(): SystemMetrics
  def close(): Unit
}

/*
* Loads JVM metrics through JMX monitoring beans.
*/
class JMXMetricsProvider extends MetricsProvider {
  val gcRateIntervalMillis = 30000
  val runtime = ManagementFactory.getRuntimeMXBean
  val os = ManagementFactory.getOperatingSystemMXBean
  val threads = ManagementFactory.getThreadMXBean
  val memory = ManagementFactory.getMemoryMXBean
  val garbageCollectors = ManagementFactory.getGarbageCollectorMXBeans

  var previousGcTimestamp: Long = runtime.getStartTime
  var previousGcCount: Long = 0L
  var previousGcTime: Long = 0L
  var gcCountPerMinute: Double = 0.0
  var gcTimePercent: Double = 0.0

  def currentMetrics(): SystemMetrics = {

    val heap = memory.getHeapMemoryUsage

    updateGcMetrics()

    SystemMetrics(
      runningActors = 0,
      startTime = runtime.getStartTime,
      upTime = runtime.getUptime,
      availableProcessors = os.getAvailableProcessors,
      daemonThreadCount = threads.getDaemonThreadCount,
      threadCount = threads.getThreadCount,
      peakThreadCount = threads.getPeakThreadCount,
      committedHeap = heap.getCommitted,
      maxHeap = heap.getMax,
      usedHeap = heap.getUsed,
      committedNonHeap = memory.getNonHeapMemoryUsage.getCommitted,
      maxNonHeap = memory.getNonHeapMemoryUsage.getMax,
      usedNonHeap = memory.getNonHeapMemoryUsage.getUsed,
      gcCountPerMinute = gcCountPerMinute,
      gcTimePercent = gcTimePercent,
      systemLoadAverage = systemLoadAverage,
      additional = additionalMetrics)
  }

  def additionalMetrics: Option[AdditionalSystemMetrics] = None

  def systemLoadAverage: Double = os.getSystemLoadAverage

  private def updateGcMetrics() {
    import scala.collection.JavaConverters._
    val timestamp = System.currentTimeMillis
    val timeMillisDelta = timestamp - previousGcTimestamp
    if (timeMillisDelta >= gcRateIntervalMillis) {
      val counts = for {
        gc ← garbageCollectors.asScala if gc.isValid
      } yield gc.getCollectionCount
      val totalCounts = counts.sum

      gcCountPerMinute = (totalCounts - previousGcCount).toDouble * 1000 * 60 / timeMillisDelta

      val times = for {
        gc ← garbageCollectors.asScala if gc.isValid
      } yield gc.getCollectionTime
      val totalTime = times.sum

      gcTimePercent = (totalTime - previousGcTime).toDouble * 100.0 / timeMillisDelta

      previousGcCount = totalCounts
      previousGcTime = totalTime
      previousGcTimestamp = timestamp
    }
  }

  def close() {}
}

object MetricsProvider {
  /*
  * Instantiates Sigar metrics provider. If it fails it will return fallback implementation (JMXMetricsProvider).
  */
  def apply(log: LoggingAdapter): MetricsProvider = {
    try {
      new SigarMetricsProvider(log)
    } catch {
      case e @ (_: NoClassDefFoundError | _: UnsatisfiedLinkError) ⇒
        log.warning("Could not load SigarMetricsProvider - using fallback JMXMetricsProvider.")
        new JMXMetricsProvider
    }
  }
}
