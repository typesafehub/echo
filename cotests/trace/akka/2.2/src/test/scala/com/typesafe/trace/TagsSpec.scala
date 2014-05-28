/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */

package com.typesafe.trace

import akka.actor._
import com.typesafe.trace.test.EchoTraceSpec
import java.util.concurrent.CountDownLatch

object TagsSpec {
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

  val config = """
    activator.trace.tags {
      "/user/tags*" = ["default", "tags"]
      "/user/tags1" = "1"
      "/user/tags2" = "2"
      "*" = "all"
    }
  """
}

class Akka22TagsSpec extends EchoTraceSpec(TagsSpec.config) {
  import TagsSpec._

  "Tags" must {

    "be attached to actor info" in {
      val latch = new CountDownLatch(1)
      val actor1 = system.actorOf(Props[TestActor], "tags1") // 10 events
      val actor2 = system.actorOf(Props[TestActor], "tags2") // 10 events

      actor2 ! latch // 3 events
      actor1 ! Forward(CountDown, actor2) // 6 events

      latch.await(timeoutHandler.time, timeoutHandler.unit) should be(true)
      actor1 ! PoisonPill // 9 events
      actor2 ! PoisonPill // 9 events

      eventCheck()
    }

  }
}
