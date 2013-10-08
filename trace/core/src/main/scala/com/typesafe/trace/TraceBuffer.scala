/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */

package com.typesafe.trace

import java.util.concurrent.atomic.{ AtomicBoolean, AtomicInteger, AtomicLong }
import java.util.concurrent.ConcurrentLinkedQueue
import scala.collection.mutable.ListBuffer
import System.{ currentTimeMillis ⇒ now }

/**
 * Store trace events in a global buffer to reduce the number of extra
 * messages added to the system when sending to the TraceEventHandlers.
 * Events are sent off when either a size or time limit are reached.
 * Set the size limit to 0 to bypass the buffering.
 */
class TraceBuffer(sizeLimit: Int, timeLimit: Int, sendOff: Batch ⇒ Unit) {

  private val queue = new ConcurrentLinkedQueue[TraceEvents]
  private val count = new AtomicInteger(0)
  private val timestamp = new AtomicLong(System.currentTimeMillis)
  private val sending = new AtomicBoolean(false)

  def add(traceEvents: TraceEvents): Unit = {
    if (sizeLimit == 0 && !sending.get) {
      sendOff(Batch(Seq(traceEvents)))
    } else {
      queue.add(traceEvents)
      val size = count.incrementAndGet
      if (size > sizeLimit || (now - timestamp.get) > timeLimit) send()
    }
  }

  def send(): Unit = {
    if (sending.compareAndSet(false, true)) {
      try {
        val buffer = ListBuffer.empty[TraceEvents]
        val drained = drain(buffer.+=)
        sendOff(Batch(buffer.toList))
        count.addAndGet(-drained)
        timestamp.set(System.currentTimeMillis)
      } finally {
        sending.set(false)
      }
    }
  }

  def drain(f: TraceEvents ⇒ Unit): Int = {
    var n = 0
    var e = queue.poll()
    while (e ne null) {
      f(e)
      n += 1
      e = queue.poll()
    }
    n
  }
}
