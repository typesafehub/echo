/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */

package com.typesafe.trace

import com.typesafe.trace.test.EchoCollectSpec

class Akka20TraceTreeSpec extends TraceTreeSpec {
  val createCount = 27
}

class Akka21TraceTreeSpec extends TraceTreeSpec {
  val createCount = 10
}

class Akka22TraceTreeSpec extends TraceTreeSpec {
  val createCount = 10
}

class Akka23Scala210TraceTreeSpec extends Akka23TraceTreeSpec
class Akka23Scala211TraceTreeSpec extends Akka23TraceTreeSpec

abstract class Akka23TraceTreeSpec extends TraceTreeSpec {
  val createCount = 10
}

abstract class TraceTreeSpec extends EchoCollectSpec {
  def createCount: Int

  "TraceTree" must {

    "build tree from single trace event" in {
      val event = TraceEvent(SystemStarted(System.currentTimeMillis), "system")
      val tree = TraceTree(Seq(event))
      tree.root should not be (None)
      tree.root.get.event should be(event)
      tree.root.get.children.size should be(0)
    }

    "build tree for simple tell trace" in {
      eventCheck("setup", expected = 2 * createCount) {
        // ignore events
      }

      eventCheck("tree", expected = 6) {
        val traceId = eventsOf[ActorCompleted].head.trace
        val trace = allEvents.filter(_.trace == traceId).toSeq
        val tree = TraceTree(trace)

        tree.root should not be (None)
        tree.root.get.event.annotation.getClass should be(classOf[ActorTold])
        tree.root.get.children.head.event.annotation.getClass should be(classOf[ActorReceived])
        tree.root.get.children.last.event.annotation.getClass should be(classOf[ActorCompleted])
        tree.root.get.children.head.children.size should be(0)
        tree.root.get.children.last.children.size should be(0)
      }

      eventCheck("poison", expected = 18) {
        // ignore events
      }
    }

    "build tree for 2 branches" in {
      eventCheck("setup", expected = 3 * createCount) {
        // ignore events
      }

      eventCheck("tree", expected = 9) {
        val traceId = eventsOf[ActorCompleted].head.trace
        val trace = allEvents.filter(_.trace == traceId).toSeq
        val tree = TraceTree(trace)

        tree.root should not be (None)
        tree.root.get.event.annotation.getClass should be(classOf[ActorTold])
        tree.root.get.children.size should be(4)
        tree.root.get.children.head.event.annotation.getClass should be(classOf[ActorReceived])
        tree.root.get.children.last.event.annotation.getClass should be(classOf[ActorCompleted])

        val aToldB = tree.root.get.children(1)
        aToldB.event.annotation.getClass should be(classOf[ActorTold])
        aToldB.children.head.event.annotation.getClass should be(classOf[ActorReceived])
        aToldB.children.last.event.annotation.getClass should be(classOf[ActorCompleted])

        val aToldC = tree.root.get.children(2)
        aToldC.event.annotation.getClass should be(classOf[ActorTold])
        aToldC.children.head.event.annotation.getClass should be(classOf[ActorReceived])
        aToldC.children.last.event.annotation.getClass should be(classOf[ActorCompleted])
      }

      eventCheck("poison", expected = 27) {
        // ignore events
      }
    }

    "build partial tree when missing parent" in {
      eventCheck("setup", expected = 3 * createCount) {
        // ignore events
      }

      eventCheck("tree", expected = 9) {
        val traceId = eventsOf[ActorCompleted].head.trace
        val trace = allEvents.filter(_.trace == traceId).toSeq
        val partialTrace = trace.filter { e ⇒
          e.annotation match {
            case x: ActorTold if x.info.path contains "/user/b" ⇒ false
            case _ ⇒ true
          }
        }

        val tree = TraceTree(partialTrace)

        tree.root should not be (None)
        tree.root.get.event.annotation.getClass should be(classOf[ActorTold])
        tree.root.get.children.size should be(3)
        tree.root.get.children.head.event.annotation.getClass should be(classOf[ActorReceived])
        tree.root.get.children.last.event.annotation.getClass should be(classOf[ActorCompleted])

        val aToldC = tree.root.get.children(1)
        aToldC.event.annotation.getClass should be(classOf[ActorTold])
        aToldC.children.head.event.annotation.getClass should be(classOf[ActorReceived])
        aToldC.children.last.event.annotation.getClass should be(classOf[ActorCompleted])
      }

      eventCheck("poison", expected = 27) {
        // ignore events
      }
    }
  }
}
