/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */
package com.typesafe.trace.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.TimeZone

object UtcDateFormat {
  val UTC = TimeZone.getTimeZone("UTC")
}

/**
 * This class is not thread safe, use [[com.typesafe.trace.util.ThreadUtcDateFormat]]
 * for concurrent access.
 */
class UtcDateFormat(pattern: String) extends SimpleDateFormat(pattern) {

  // set the calendar to use UTC
  calendar = Calendar.getInstance(UtcDateFormat.UTC)
  calendar.setLenient(false)

}

/**
 * ThreadLocal holder of a [[com.typesafe.trace.util.UtcDateFormat]].
 */
object ThreadUtcDateFormat {
  def apply(pattern: String): ThreadLocal[UtcDateFormat] = new ThreadLocal[UtcDateFormat]() {
    override protected def initialValue(): UtcDateFormat = new UtcDateFormat(pattern)
  }
}
