/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */
package com.typesafe.trace.util

import java.util.TimeZone
import org.scalatest.{ WordSpec, MustMatchers }

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class UtcDateFormatSpec extends WordSpec with MustMatchers {
  val utcDateFormat = new UtcDateFormat("yyyy-MM-dd'T'HH:mm")

  "UtcDateFormat parsing" must {
    "throw an exception for incorrect date format" in {
      an[java.text.ParseException] must be thrownBy (utcDateFormat.parse("2011-12-30Tx7:22"))
      an[java.text.ParseException] must be thrownBy (utcDateFormat.parse("2011-12-30T77:22"))
      an[java.text.ParseException] must be thrownBy (utcDateFormat.parse("2011-12-38T14:22"))
      an[java.text.ParseException] must be thrownBy (utcDateFormat.parse("2011-16-27T14:22"))
    }
  }

  "UtcDateFormat time zone type" must {
    "be of type UTC" in {
      utcDateFormat.getTimeZone must equal(TimeZone.getTimeZone("UTC"))
    }
  }
}
