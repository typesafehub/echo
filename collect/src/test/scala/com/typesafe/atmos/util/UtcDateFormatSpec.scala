/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */
package com.typesafe.atmos.util

import java.util.TimeZone
import org.scalatest.matchers.{ ShouldMatchers, MustMatchers }
import org.scalatest.WordSpec

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class UtcDateFormatSpec extends WordSpec with MustMatchers with ShouldMatchers {
  val utcDateFormat = new UtcDateFormat("yyyy-MM-dd'T'HH:mm")

  "UtcDateFormat parsing" must {
    "throw an exception for incorrect date format" in {
      evaluating(utcDateFormat.parse("2011-12-30Tx7:22")) should produce[java.text.ParseException]
      evaluating(utcDateFormat.parse("2011-12-30T77:22")) should produce[java.text.ParseException]
      evaluating(utcDateFormat.parse("2011-12-38T14:22")) should produce[java.text.ParseException]
      evaluating(utcDateFormat.parse("2011-16-27T14:22")) should produce[java.text.ParseException]
    }
  }

  "UtcDateFormat time zone type" must {
    "be of type UTC" in {
      utcDateFormat.getTimeZone must equal(TimeZone.getTimeZone("UTC"))
    }
  }
}
