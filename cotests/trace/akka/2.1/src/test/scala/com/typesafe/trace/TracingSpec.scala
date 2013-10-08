/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */

package com.typesafe.trace

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.trace.test.{ EchoTraceSpec, CotestSyncSpec }
import com.typesafe.trace.util.ExpectedFailureException
import java.util.concurrent.CountDownLatch
import scala.concurrent.Await

object TracingSpec {
  case object Boom

  class Forwarder(actor: ActorRef) extends Actor {
    def receive = {
      case message: String ⇒
        Tracer(context.system).record("flow", "forwarding message")
        actor ! ("forward " + message)
    }
  }

  class Receiver extends Actor {
    var latch: Option[CountDownLatch] = None

    def receive = {
      case cdl: CountDownLatch ⇒
        Tracer(context.system).record("flow", "received latch")
        latch = Some(cdl)
      case message: String ⇒
        Tracer(context.system).record("flow", "counting down")
        latch foreach (_.countDown)
    }
  }

  class Failer extends Actor {
    def receive = {
      case Boom            ⇒ throw new ExpectedFailureException("Boom")
      case message: String ⇒ sender ! ("got " + message)
    }
  }
}

class Akka21TracingSpec extends EchoTraceSpec {
  import TracingSpec._

  "Tracing" must {

    "store all trace events in memory" in {
      val latch = new CountDownLatch(1)

      // 1 event: 1 group started
      Tracer(system).startGroup("test")

      // 31 events: 1 marker, 3 system.actorOf
      Tracer(system).record("flow", "creating actors")
      val receiver = system.actorOf(Props[Receiver], "test.Receiver")
      val forwarder1 = system.actorOf(Props(new Forwarder(receiver)), "test.Forwarder1")
      val forwarder2 = system.actorOf(Props(new Forwarder(forwarder1)), "test.Forwarder2")

      // 4 events: 1 group started, 1 group ended, 2 told
      // 14 events: 3 for received latch, 4 for each forwarder, 3 for counting down
      Tracer(system).group("messages") {
        receiver ! latch
        forwarder2 ! "hi"
        latch.await(timeoutHandler.time, timeoutHandler.unit) must be(true)
      }

      // 28 events: 1 marker, 3 poison pills
      Tracer(system).record("flow", "stopping actors")
      forwarder2 ! PoisonPill
      forwarder1 ! PoisonPill
      receiver ! PoisonPill

      // 1 event: 1 group ended
      Tracer(system).endGroup("test")

      eventCheck()
    }

    "record actor failed events" in {
      val latch = new CountDownLatch(1)

      // 1 event: 1 group started
      Tracer(system).startGroup("test")

      // 10 events: system.actorOf
      val failer = system.actorOf(Props[Failer], "failer")

      // 9 events: failing message
      failer ! Boom

      // 17 events: ask and reply
      implicit val timeout = Timeout(timeoutHandler.time, timeoutHandler.unit)
      Await.result(failer ? "message", timeout.duration) match {
        case "got message" ⇒ ()
        case _             ⇒ throw new Exception("Expected 'got message'")
      }

      // 9 events: poison pill
      failer ! PoisonPill

      // 1 event: 1 group ended
      Tracer(system).endGroup("test")

      eventCheck()
    }
  }
}
