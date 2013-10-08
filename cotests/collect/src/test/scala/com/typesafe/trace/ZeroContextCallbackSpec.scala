/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */

package com.typesafe.trace

import com.typesafe.trace.test.EchoCollectSpec

class Akka20ZeroContextCallbackSpec extends ZeroContextCallbackSpec {
  val createCount = 27
}

class Akka21ZeroContextCallbackSpec extends ZeroContextCallbackSpec {
  val createCount = 10
}

class Akka22Scala210ZeroContextCallbackSpec extends Akka22ZeroContextCallbackSpec
class Akka22Scala211ZeroContextCallbackSpec extends Akka22ZeroContextCallbackSpec

abstract class Akka22ZeroContextCallbackSpec extends ZeroContextCallbackSpec {
  val createCount = 10
}

abstract class ZeroContextCallbackSpec extends EchoCollectSpec {
  def createCount: Int

  "Zero context callbacks" must {

    "stay as zero context for message sends to be traceable" in {
      eventCheck("setup", expected = createCount + 3) {
        // ignore events
      }

      eventCheck("zero", expected = 12) {
        countTraces must be(4)
        countEventsOf[ActorTold] must be(4)
        countEventsOf[ActorReceived] must be(4)
        countEventsOf[ActorCompleted] must be(4)
      }
    }

  }
}
