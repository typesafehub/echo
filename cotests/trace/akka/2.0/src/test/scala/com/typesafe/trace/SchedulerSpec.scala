/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */

package com.typesafe.trace

import akka.actor._
import akka.util.Duration
import com.typesafe.trace.test.AtmosTraceSpec
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.MILLISECONDS

object SchedulerSpec {

  class Delayer(actor: ActorRef) extends Actor {
    def receive = {
      case message: String ⇒
        context.system.scheduler.scheduleOnce(Duration(13, MILLISECONDS), actor, message)
        val runnable = new Runnable {
          def run { actor ! message.toUpperCase }
        }
        context.system.scheduler.scheduleOnce(Duration(17, MILLISECONDS), runnable)
    }
  }

  class Receiver(latch: CountDownLatch) extends Actor {
    def receive = {
      case message: String ⇒
        latch.countDown()
    }
  }

}

class Akka20SchedulerSpec extends AtmosTraceSpec {
  import SchedulerSpec._

  "Tracing of Scheduler" must {

    "continue delayed message flow" in {
      val latch = new CountDownLatch(2)
      val receiver = system.actorOf(Props(new Receiver(latch)), "receiver") // 27 events
      val delayer = system.actorOf(Props(new Delayer(receiver)), "delayer") // 27 events

      eventCheck("setup")

      delayer ! "hello"
      latch.await(timeoutHandler.time, timeoutHandler.unit) must be(true)

      eventCheck("main")

      receiver ! PoisonPill // 9 events
      delayer ! PoisonPill // 9 events

      eventCheck("poison")
    }
  }

}
