/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */
package com.typesafe.atmos.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.TimeZone

object UtcDateFormat {
  val UTC = TimeZone.getTimeZone("UTC")
}

/**
 * This class is not thread safe, use [[com.typesafe.atmos.util.ThreadUtcDateFormat]]
 * for concurrent access.
 */
class UtcDateFormat(pattern: String) extends SimpleDateFormat(pattern) {

  // set the calendar to use UTC
  calendar = Calendar.getInstance(UtcDateFormat.UTC)
  calendar.setLenient(false)

}

/**
 * ThreadLocal holder of a [[com.typesafe.atmos.util.UtcDateFormat]].
 */
object ThreadUtcDateFormat {
  def apply(pattern: String): ThreadLocal[UtcDateFormat] = new ThreadLocal[UtcDateFormat]() {
    override protected def initialValue(): UtcDateFormat = new UtcDateFormat(pattern)
  }
}
