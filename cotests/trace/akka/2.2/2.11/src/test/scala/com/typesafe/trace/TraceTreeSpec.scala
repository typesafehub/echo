/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */

package com.typesafe.trace

import akka.actor._
import com.typesafe.trace.test.EchoTraceSpec

object TraceTreeSpec {

  class TestActor(sendTo: Seq[ActorRef]) extends Actor {
    def this() = this(Nil)
    def receive = {
      case msg ⇒ sendTo foreach (_ ! msg)
    }
  }
}

class Akka22Scala211TraceTreeSpec extends EchoTraceSpec {
  import TraceTreeSpec._

  "TraceTree" must {

    "build tree from single trace event" in {
      // no trace
    }

    "build tree for simple tell trace" in {
      val b = system.actorOf(Props[TestActor], "b")
      val a = system.actorOf(Props(new TestActor(Seq(b))), "a")

      eventCheck("setup")

      a ! "msg"

      eventCheck("tree")

      a ! PoisonPill
      b ! PoisonPill

      eventCheck("poison")
    }

    "build tree for 2 branches" in {
      val b = system.actorOf(Props[TestActor], "b")
      val c = system.actorOf(Props[TestActor], "c")
      val a = system.actorOf(Props(new TestActor(Seq(b, c))), "a")

      eventCheck("setup")

      a ! "msg"

      eventCheck("tree")

      a ! PoisonPill
      b ! PoisonPill
      c ! PoisonPill

      eventCheck("poison")
    }

    "build partial tree when missing parent" in {
      val b = system.actorOf(Props[TestActor], "b")
      val c = system.actorOf(Props[TestActor], "c")
      val a = system.actorOf(Props(new TestActor(Seq(b, c))), "a")

      eventCheck("setup")

      a ! "msg"

      eventCheck("tree")

      a ! PoisonPill
      b ! PoisonPill
      c ! PoisonPill

      eventCheck("poison")
    }
  }
}
