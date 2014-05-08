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

class Akka22ZeroContextCallbackSpec extends ZeroContextCallbackSpec {
  val createCount = 10
}

class Akka23Scala210ZeroContextCallbackSpec extends Akka23ZeroContextCallbackSpec
class Akka23Scala211ZeroContextCallbackSpec extends Akka23ZeroContextCallbackSpec

abstract class Akka23ZeroContextCallbackSpec extends ZeroContextCallbackSpec {
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
        countTraces should be(4)
        countEventsOf[ActorTold] should be(4)
        countEventsOf[ActorReceived] should be(4)
        countEventsOf[ActorCompleted] should be(4)
      }
    }

  }
}
