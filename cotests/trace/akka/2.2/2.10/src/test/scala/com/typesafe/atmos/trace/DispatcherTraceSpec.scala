/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */

package com.typesafe.atmos.trace

import com.typesafe.atmos.test.AtmosTraceSpec

object DispatcherTraceSpec {
  val config = """
    atmos.trace.use-dispatcher-monitor = on
  """
}

class Akka22Scala210DispatcherTraceSpec extends AtmosTraceSpec(DispatcherTraceSpec.config) {
  "Dispatcher trace" must {
    "generate dispatcher status events" in {
      eventCheck()
    }
  }
}
