/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */

package com.typesafe.atmos.trace

import com.typesafe.atmos.test.AtmosCollectSpec

class Akka20UnhandledMessageSpec extends UnhandledMessageSpec {
  val createCount = 27
}

class Akka21UnhandledMessageSpec extends UnhandledMessageSpec {
  val createCount = 10
}

class Akka22Scala210UnhandledMessageSpec extends Akka22UnhandledMessageSpec
class Akka22Scala211UnhandledMessageSpec extends Akka22UnhandledMessageSpec

abstract class Akka22UnhandledMessageSpec extends UnhandledMessageSpec {
  val createCount = 10
}

abstract class UnhandledMessageSpec extends AtmosCollectSpec {
  def createCount: Int

  "Unhandled message tracing" must {

    "record unhandled message events" in {
      eventCheck(expected = createCount + 13) {
        // 4 events: ActorTold, ActorReceived, EventStreamUnhandledMessage, ActorCompleted
        // 9 events for poison pill
        countEventsOf[EventStreamUnhandledMessage] must be(1)
      }
    }

  }
}
