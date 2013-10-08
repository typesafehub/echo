/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */

package com.typesafe.atmos.util

import java.util.concurrent.atomic.AtomicInteger

/**
 * Counter that returns 0, 1, ..., count - 1, in a cycle.
 */
class CyclicCounter(val count: Int) {
  private val counter = new AtomicInteger(count)

  def next(): Int = {
    val value = counter.getAndIncrement() % count
    if (value == 0) counter.getAndAdd(-count)
    value
  }
}

object CyclicCounter {
  def apply(count: Int) = new CyclicCounter(count)
}
