package com.typesafe.atmos.trace

import akka.event.LoggingAdapter
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.InputStreamReader
import org.hyperic.sigar.Sigar
import scala.collection.mutable.{ Set ⇒ MutableSet }

/**
 * Loads metrics of a better quality with Hyperic Sigar (native library).
 * Sigar Home page: http://support.hyperic.com/display/SIGAR/Home
 * Sigar License: Apache License, Version 2.0.
 *
 * Note that Sigar use a native library, and the directory of the native libs
 * must be defined in -Djava.library.path (or be located in same directory as sigar.jar)
 */
class SigarMetricsProvider(log: LoggingAdapter) extends JMXMetricsProvider {
  val sigar: Sigar = new Sigar
  // do something initially to make sure that the native library can be loaded
  val pid = sigar.getPid
  var contextSwitchesAvailable = true
  var previousContextSwitchesTimestamp: Long = -1L
  var previousContextSwitches: Long = -1L
  var previousNetStatTimestamp: Long = -1L
  var previousNetRxBytes: Long = -1L
  var previousNetTxBytes: Long = -1L
  val logged: MutableSet[String] = MutableSet.empty

  override def systemLoadAverage: Double = {
    safe(sigar.getLoadAverage()(0), super.systemLoadAverage, "systemLoadAverage")
  }

  /**
   * Reading context switches from "/proc/stat" if available, otherwise
   * returns -1. Only available for Linux.
   */
  def contextSwitchesPerSecond: Long = {
    if (contextSwitchesAvailable) {
      var reader: BufferedReader = null
      try {
        reader = new BufferedReader(new InputStreamReader(new FileInputStream("/proc/stat")))
        var ctxt = -1L
        var line = reader.readLine()

        while ((line ne null) && ctxt == -1L) {
          if (line.startsWith("ctxt")) {
            val value = line.substring(line.indexOf(" ") + 1)
            ctxt = value.toLong
          } else {
            line = reader.readLine()
          }
        }

        val now = System.currentTimeMillis
        val contextSwitchesPerSecond =
          if (previousContextSwitches < 0L || ctxt < 0L) -1L
          else (ctxt - previousContextSwitches) * 1000 / (now - previousContextSwitchesTimestamp)
        if (ctxt >= 0L) {
          previousContextSwitches = ctxt
          previousContextSwitchesTimestamp = now
        }
        contextSwitchesPerSecond

      } catch {
        case e: Exception ⇒
          contextSwitchesAvailable = false
          log.info("Context switches information not available. Due to [{}]", e.getMessage)
          -1L
      } finally {
        if (reader ne null) reader.close()
      }

    } else {
      -1L
    }

  }

  def netInterfaceList: Set[String] = {
    safe(sigar.getNetInterfaceList.toSet, Set.empty, "netInterfaceList")
  }

  def cpuMetrics: CpuSystemMetrics = {
    // note that it is very important to only invoke sigar.getCpuPerc
    // once per measurement point, since it compares previous with current
    def cpuPerc = {
      val cpuPerc = sigar.getCpuPerc
      (cpuPerc.getUser, cpuPerc.getSys, cpuPerc.getCombined)
    }
    val (cpuUser, cpuSys, cpuCombined) = safe(cpuPerc, (0.0, 0.0, 0.0), "cpuPerc")

    // array of load average for 1, 5 and 15 minutes
    val loadAverage = safe(sigar.getLoadAverage, Array(0.0, 0.0, 0.0), "loadAverage")
    CpuSystemMetrics(
      cpuUser = cpuUser,
      cpuSys = cpuSys,
      cpuCombined = cpuCombined,
      pidCpu = safe(sigar.getProcCpu(pid).getPercent, 0.0, "pidCpu"),
      loadAverage1min = loadAverage(0),
      loadAverage5min = loadAverage(1),
      loadAverage15min = loadAverage(2),
      contextSwitches = contextSwitchesPerSecond)
  }

  def memoryMetrics: MemorySystemMetrics = {
    def swap = {
      val swap = sigar.getSwap
      (swap.getPageIn, swap.getPageOut)
    }
    val (memSwapPageIn, memSwapPageOut) = safe(swap, (0L, 0L), "memorySwap")
    MemorySystemMetrics(
      memUsage = safe(sigar.getMem.getUsedPercent, 0.0, "memoryUsed"),
      memSwapPageIn = memSwapPageIn,
      memSwapPageOut = memSwapPageOut)
  }

  def networkMetrics: NetworkSystemMetrics = {
    var netRxBytes = 0L
    var netTxBytes = 0L
    var netRxErrors = 0L
    var netTxErrors = 0L

    val now = System.currentTimeMillis

    for (each ← netInterfaceList; net ← safe(Some(sigar.getNetInterfaceStat(each)), None, "netInterfaceStat-" + each)) {
      netRxBytes += net.getRxBytes
      netTxBytes += net.getTxBytes
      netRxErrors += net.getRxErrors
      netTxErrors += net.getTxErrors
    }

    val netRxBytesPerSecond =
      if (previousNetStatTimestamp < 0L) 0L
      else (netRxBytes - previousNetRxBytes) * 1000 / (now - previousNetStatTimestamp)
    val netTxBytesPerSecond =
      if (previousNetStatTimestamp < 0L) 0L
      else (netTxBytes - previousNetTxBytes) * 1000 / (now - previousNetStatTimestamp)
    previousNetRxBytes = netRxBytes
    previousNetTxBytes = netTxBytes
    previousNetStatTimestamp = now

    def tcp = {
      val tcp = sigar.getTcp
      (tcp.getCurrEstab, tcp.getEstabResets)
    }
    val (tcpCurrEstab, tcpEstabResets) = safe(tcp, (0L, 0L), "tcp")

    NetworkSystemMetrics(
      tcpCurrEstab = tcpCurrEstab,
      tcpEstabResets = tcpEstabResets,
      netRxBytesRate = netRxBytesPerSecond,
      netTxBytesRate = netTxBytesPerSecond,
      netRxErrors = netRxErrors,
      netTxErrors = netTxErrors)
  }

  def safe[T](fun: ⇒ T, defaultValue: ⇒ T, errorId: String): T = {
    try {
      fun
    } catch {
      case e: Exception ⇒
        if (!logged.contains(errorId)) {
          logged += errorId
          log.warning("Couldn't monitor [{}]. Due to [{}]", errorId, e.getMessage)
        }
        defaultValue
    }
  }

  override def additionalMetrics: Option[AdditionalSystemMetrics] = {
    try {
      Some(AdditionalSystemMetrics(
        cpu = cpuMetrics,
        memory = memoryMetrics,
        network = networkMetrics))
    } catch {
      case e: Exception ⇒
        log.warning("Couldn't monitor additional system metrics. Due to [{}]", e.getMessage)
        None
    }
  }

  override def close() {
    try {
      sigar.close()
    } catch {
      case _: Exception ⇒
    }
    super.close()
  }

}
