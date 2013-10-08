/**
 * Copyright (C) 2009-2011 Scalable Solutions AB <http://scalablesolutions.se>
 */
package com.typesafe.atmos.trace

import com.typesafe.atmos.test.AtmosCollectSpec

class Akka20EventStreamSpec extends EventStreamSpec
class Akka21EventStreamSpec extends EventStreamSpec
class Akka22Scala210EventStreamSpec extends EventStreamSpec
class Akka22Scala211EventStreamSpec extends EventStreamSpec

abstract class EventStreamSpec extends AtmosCollectSpec {

  "Event stream tracing" must {

    "capture all errors and warnings" in {
      eventCheck(expected = 8) {
        countEventsOf[EventStreamError] must be(4)
        countEventsOf[EventStreamWarning] must be(4)
      }
    }

  }
}
