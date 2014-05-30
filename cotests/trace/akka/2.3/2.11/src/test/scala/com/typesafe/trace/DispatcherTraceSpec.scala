/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */

package com.typesafe.trace

import com.typesafe.trace.test.EchoTraceSpec

object DispatcherTraceSpec {
  val config = """
    activator.trace.use-dispatcher-monitor = on
  """
}

class Akka23Scala211DispatcherTraceSpec extends EchoTraceSpec(DispatcherTraceSpec.config) {
  "Dispatcher trace" must {
    "generate dispatcher status events" in {
      eventCheck()
    }
  }
}
