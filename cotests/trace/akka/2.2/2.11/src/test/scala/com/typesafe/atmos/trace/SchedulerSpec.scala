/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */

package com.typesafe.atmos.trace

import akka.actor._
import com.typesafe.atmos.test.AtmosTraceSpec
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.MILLISECONDS
import scala.concurrent.duration.Duration

object SchedulerSpec {

  class Delayer(actor: ActorRef) extends Actor {
    def receive = {
      case message: String ⇒
        context.system.scheduler.scheduleOnce(Duration(13, MILLISECONDS), actor, message)(context.dispatcher)
        val runnable = new Runnable {
          def run { actor ! message.toUpperCase }
        }
        context.system.scheduler.scheduleOnce(Duration(17, MILLISECONDS), runnable)(context.dispatcher)
    }
  }

  class Receiver(latch: CountDownLatch) extends Actor {
    def receive = {
      case message: String ⇒
        latch.countDown()
    }
  }

}

class Akka22Scala211SchedulerSpec extends AtmosTraceSpec {
  import SchedulerSpec._

  "Tracing of Scheduler" must {

    "continue delayed message flow" in {
      val latch = new CountDownLatch(2)
      val receiver = system.actorOf(Props(new Receiver(latch)), "receiver") // 10 events
      val delayer = system.actorOf(Props(new Delayer(receiver)), "delayer") // 10 events

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
