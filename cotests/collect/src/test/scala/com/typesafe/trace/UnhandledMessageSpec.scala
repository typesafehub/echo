/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */

package com.typesafe.trace

import com.typesafe.trace.test.EchoCollectSpec

class Akka20UnhandledMessageSpec extends UnhandledMessageSpec {
  val createCount = 27
}

class Akka21UnhandledMessageSpec extends UnhandledMessageSpec {
  val createCount = 10
}

class Akka22UnhandledMessageSpec extends UnhandledMessageSpec {
  val createCount = 10
}

class Akka23Scala210UnhandledMessageSpec extends Akka23UnhandledMessageSpec
class Akka23Scala211UnhandledMessageSpec extends Akka23UnhandledMessageSpec

abstract class Akka23UnhandledMessageSpec extends UnhandledMessageSpec {
  val createCount = 10
}

abstract class UnhandledMessageSpec extends EchoCollectSpec {
  def createCount: Int

  "Unhandled message tracing" must {

    "record unhandled message events" in {
      eventCheck(expected = createCount + 13) {
        // 4 events: ActorTold, ActorReceived, EventStreamUnhandledMessage, ActorCompleted
        // 9 events for poison pill
        countEventsOf[EventStreamUnhandledMessage] should be(1)
      }
    }

  }
}
