/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */

package com.typesafe.trace

import com.typesafe.trace.test.EchoCollectSpec
import scala.concurrent.duration._

class Akka22Scala210DispatcherTraceSpec extends AkkaDispatcherTraceSpec

abstract class AkkaDispatcherTraceSpec extends EchoCollectSpec {
  "Dispatcher trace" must {
    "generate dispatcher status events" in {
      eventCheck(expected = 1) {
        countEventsOf[DispatcherStatus] must be >= 1
      }
    }
  }
}
