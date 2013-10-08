/**
 *  Copyright (C) 2011-2013 Typesafe <http://typesafe.com/>
 */
package com.typesafe.atmos.trace.send

import com.google.protobuf.CodedOutputStream
import com.typesafe.atmos.trace.{ Batch, TraceSender }
import com.typesafe.atmos.util.ProtobufConverter
import java.net.{ ConnectException, InetAddress, Socket }
import java.util.concurrent.atomic.{ AtomicBoolean, AtomicLong }
import java.util.concurrent.{ LinkedBlockingQueue, ThreadFactory, ThreadPoolExecutor, TimeUnit }

class ProtobufTraceSender(port: Int, capacity: Int, retry: Boolean, daemonic: Boolean, warn: Boolean) extends TraceSender {

  val address = InetAddress.getByName(null)

  private val executor = {
    val workQueue = {
      if (capacity > 0) new LinkedBlockingQueue[Runnable](capacity)
      else new LinkedBlockingQueue[Runnable]()
    }
    val threadFactory = new TraceSenderThreadFactory("atmos-trace-sender", daemonic)
    val rejectionHandler = new ThreadPoolExecutor.DiscardPolicy
    new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, workQueue, threadFactory, rejectionHandler)
  }

  private val running = new AtomicBoolean(true)

  private val maxRetryDelay: Long = 10 * 1000 // milliseconds

  @volatile private var connected: Boolean = false
  private var socket: Socket = _
  private var output: CodedOutputStream = _
  private var retryTime: Long = 0 // timestamp
  private var retryDelay: Long = 100 // milliseconds
  private var warned: Boolean = false

  private def printWarning(retryEnabled: Boolean = true): Unit = {
    if (warn && !warned) {
      val retryStatus = if (retryEnabled) "(retry enabled)" else ""
      println("WARN: No trace receiver on port [%s] %s" format (port, retryStatus))
      warned = true
    }
  }

  private def resetRetry(): Unit = {
    retryTime = 0
    retryDelay = 100
    warned = false
  }

  private def backoffRetry(fromTime: Long): Unit = {
    retryDelay = math.min(retryDelay * 2, maxRetryDelay)
    retryTime = fromTime + retryDelay
    printWarning(retryEnabled = true)
  }

  private def noRetry(): Unit = {
    retryTime = Long.MaxValue
    printWarning(retryEnabled = false)
    spawnShutdown()
  }

  private def connect(): Boolean = {
    val now = System.currentTimeMillis
    if (now > retryTime && running.get) {
      try {
        socket = new Socket(address, port)
        output = CodedOutputStream.newInstance(socket.getOutputStream)
        resetRetry()
        true
      } catch {
        case e: ConnectException ⇒
          if (retry) backoffRetry(now) else noRetry()
          false // can't connect
      }
    } else {
      false // waiting for retry
    }
  }

  def send(batch: Batch): Unit = {
    if (running.get) {
      try {
        executor.execute(new Runnable {
          def run = write(batch)
        })
      } catch {
        case e: Exception ⇒ // assume shutdown
      }
    }
  }

  private def write(batch: Batch): Unit = {
    val alreadyConnected = connected // volatile read
    val isConnected = alreadyConnected || connect()
    val sent = if (isConnected) {
      try {
        val message = ProtobufConverter.toProto(batch)
        output.writeRawVarint32(message.getSerializedSize)
        message.writeTo(output)
        output.flush()
        true
      } catch {
        case e: Exception ⇒ false // assume disconnected, drop traces
      }
    } else {
      false // no connection at this time, drop traces
    }
    connected = sent // volatile write
  }

  def shutdown(): Unit = {
    if (running.compareAndSet(true, false)) {
      runShutdown()
    }
  }

  private def spawnShutdown(): Unit = {
    if (running.compareAndSet(true, false)) {
      (new Thread { override def run = runShutdown() }).start()
    }
  }

  private def runShutdown(): Unit = {
    executor.shutdown()
    if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
      if (socket ne null) socket.close()
      executor.shutdownNow()
      executor.awaitTermination(1, TimeUnit.SECONDS)
    }
    if (socket ne null) socket.close()
  }
}

class TraceSenderThreadFactory(name: String, daemonic: Boolean) extends ThreadFactory {
  private[this] val counter: AtomicLong = new AtomicLong(0L)
  def newThread(runnable: Runnable): Thread = {
    val threadName = name + "-" + counter.incrementAndGet()
    val thread = new Thread(runnable, threadName)
    thread.setDaemon(daemonic)
    thread
  }
}
