/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */

package com.typesafe.atmos.trace

import akka.actor._
import com.typesafe.atmos.test.AtmosTraceSpec
import java.util.concurrent.CountDownLatch

object SamplingSpec {
  case class Forward(message: Any, actor: ActorRef)
  case object CountDown

  class TestActor extends Actor {
    var latch: Option[CountDownLatch] = None

    def receive = {
      case Forward(message, actor) ⇒
        actor ! message
      case cdl: CountDownLatch ⇒
        latch = Some(cdl)
      case CountDown ⇒
        latch foreach (_.countDown)
      case _ ⇒ () // do nothing
    }
  }

  val config = """
    atmos.trace.sampling {
      "/user/sampling1" = 3
      "/user/sampling2" = 1
    }
  """
}

class Akka21SamplingSpec extends AtmosTraceSpec(SamplingSpec.config) {
  import SamplingSpec._

  "Sampling" must {

    "only trace at sampling rate" in {
      val latch = new CountDownLatch(1)

      val actor1 = system.actorOf(Props[TestActor], "sampling1") // sampled = 1 (10 events, 1 trace)
      val actor2 = system.actorOf(Props[TestActor], "sampling2") // sampled = 1 (10 events, 1 trace)

      actor2 ! latch // sampled = 1 (3 events, 1 trace)

      actor1 ! Forward("1", actor2) // sampled = 3 (6 events, 1 trace)
      actor1 ! Forward("2", actor2) // not sampled
      actor1 ! Forward("3", actor2) // not sampled
      actor1 ! Forward("4", actor2) // sampled = 3 (6 events, 1 trace)
      actor1 ! Forward("5", actor2) // not sampled
      actor1 ! Forward("6", actor2) // not sampled

      actor1 ! Forward(CountDown, actor2) // sampled = 3 (6 events, 1 trace)
      latch.await(timeoutHandler.time, timeoutHandler.unit) must be(true)

      actor1 ! PoisonPill // not sampled
      actor2 ! PoisonPill // sampled = 1 (9 events, 1 trace)

      eventCheck()
    }

  }
}
