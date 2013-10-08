/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */

package com.typesafe.trace.util

/**
 * Creates something on demand, similar to a lazy value, but also shuts down
 * the value when no longer in use.
 */
abstract class OnDemand[A] {
  private[this] var count = 0
  private[this] var value: A = _

  def get(): A = synchronized { value }

  def access(): A = synchronized {
    if (count == 0) value = create()
    count += 1
    value
  }

  def release(): Unit = synchronized {
    if (count > 0) {
      count -= 1
      if (count == 0) {
        shutdown(value)
        value = null.asInstanceOf[A]
      }
    }
  }

  def apply(f: A ⇒ Unit) = {
    val current = get
    if (current != null) f(current)
  }

  def create(): A

  def shutdown(a: A): Unit
}
