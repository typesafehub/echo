/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */

package com.typesafe.trace

import akka.actor._
import akka.pattern.pipe
import com.typesafe.trace.test.AtmosTraceSpec
import java.util.concurrent.CountDownLatch
import scala.concurrent.duration._
import scala.concurrent.Future

object ZeroContextCallbackSpec {
  case object Msg

  class Receiver extends Actor {
    var latch: Option[CountDownLatch] = None
    def receive = {
      case cdl: CountDownLatch ⇒ latch = Some(cdl)
      case Msg                 ⇒ latch foreach (_.countDown)
    }
  }
}

class Akka21ZeroContextCallbackSpec extends AtmosTraceSpec {
  import ZeroContextCallbackSpec._
  import scala.concurrent.ExecutionContext.Implicits.global

  "Zero context callbacks" must {

    "stay as zero context for message sends to be traceable" in {
      val actor = system.actorOf(Props[Receiver], "receiver")

      val latch = new CountDownLatch(4)
      actor ! latch

      eventCheck("setup")

      system.scheduler.scheduleOnce(1.millisecond) {
        actor ! Msg
      }

      Future { actor ! Msg }

      Future { Msg } onSuccess { case msg ⇒ actor ! msg }

      Future { Msg } pipeTo actor

      latch.await(timeoutHandler.time, timeoutHandler.unit)

      eventCheck("zero")
    }
  }
}
