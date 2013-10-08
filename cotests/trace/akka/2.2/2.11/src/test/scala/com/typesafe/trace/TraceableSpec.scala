/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */

package com.typesafe.trace

import akka.actor._
import com.typesafe.trace.test.AtmosTraceSpec
import java.util.concurrent.CountDownLatch

object TraceableSpec {
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
    }
  }

  class MyActor extends Actor {
    def receive = {
      case l: CountDownLatch ⇒
        val actor2 = context.actorOf(Props[TestActor], "traceable")
        actor2 ! l
        actor2 ! CountDown
    }
  }

  val config = """
    atmos.trace.traceable {
      "/user/traceable" = true
      "/user/traceable1" = true
      "/user/traceable2" = true
      "/user/untraceable" = false
    }
  """
}

class Akka22Scala211TraceableSpec extends AtmosTraceSpec(TraceableSpec.config) {
  import TraceableSpec._

  "Traceable" must {

    "trace traceable actors" in {
      val latch = new CountDownLatch(1)

      // 10 events: system.actorOf
      val actor = system.actorOf(Props[TestActor], "traceable")

      // 6 events, 2 x told, received, completed
      actor ! latch
      actor ! CountDown

      latch.await(timeoutHandler.time, timeoutHandler.unit) must be(true)

      // 9 events: poison pill
      actor ! PoisonPill

      eventCheck()
    }

    "not trace untraceable actors" in {
      val latch = new CountDownLatch(1)

      val actor = system.actorOf(Props[TestActor], "untraceable")

      actor ! latch
      actor ! CountDown

      latch.await(timeoutHandler.time, timeoutHandler.unit) must be(true)
      actor ! PoisonPill

      eventCheck()
    }

    "trace traceable actors created in context" in {
      val latch = new CountDownLatch(1)

      val actor1 = system.actorOf(Props[MyActor], "untraceable")
      actor1 ! latch

      latch.await(timeoutHandler.time, timeoutHandler.unit) must be(true)
      actor1 ! PoisonPill

      eventCheck()
    }

    "treat untraceable actors as the empty context" in {

      // 2 events: group started and ended
      Tracer(system).group("wrap") {
        val latch = new CountDownLatch(1)

        // 20 events: 2 x system.actorOf (untraceble not recorded at all)
        val traceable1 = system.actorOf(Props[TestActor], "traceable1")
        val traceable2 = system.actorOf(Props[TestActor], "traceable2")
        val untraceable = system.actorOf(Props[TestActor], "untraceable")

        // 3 events: told, received, completed
        traceable2 ! latch

        // 6 events: 2 x told, received, completed (untraceable not recorded)
        traceable1 ! Forward(Forward(CountDown, traceable2), untraceable)

        latch.await(timeoutHandler.time, timeoutHandler.unit) must be(true)

        // 18 events: 2 x poison pill (untraceable not recorded)
        traceable1 ! PoisonPill
        untraceable ! PoisonPill
        traceable2 ! PoisonPill
      }

      eventCheck()
    }

  }
}
