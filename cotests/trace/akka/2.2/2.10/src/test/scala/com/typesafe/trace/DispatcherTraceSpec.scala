/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */

package com.typesafe.trace

import com.typesafe.trace.test.EchoTraceSpec

object DispatcherTraceSpec {
  val config = """
    atmos.trace.use-dispatcher-monitor = on
  """
}

class Akka22Scala210DispatcherTraceSpec extends EchoTraceSpec(DispatcherTraceSpec.config) {
  "Dispatcher trace" must {
    "generate dispatcher status events" in {
      eventCheck()
    }
  }
}
