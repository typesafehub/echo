/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */

package com.typesafe.trace

import com.typesafe.trace.test.EchoCollectSpec

class Akka20SchedulerSpec extends SchedulerSpec {
  val setupCount = 54
  val mainCount = 18
  val scheduledCount = 2
  val runnableCount = 1
}

class Akka21SchedulerSpec extends SchedulerSpec {
  val setupCount = 20
  val mainCount = 21
  val scheduledCount = 2
  val runnableCount = 2
}

class Akka22SchedulerSpec extends SchedulerSpec {
  val setupCount = 20
  val mainCount = 17
  val scheduledCount = 0
  val runnableCount = 2
}

class Akka23Scala210SchedulerSpec extends Akka23SchedulerSpec
class Akka23Scala211SchedulerSpec extends Akka23SchedulerSpec

abstract class Akka23SchedulerSpec extends SchedulerSpec {
  val setupCount = 20
  val mainCount = 17
  val scheduledCount = 0
  val runnableCount = 2
}

abstract class SchedulerSpec extends EchoCollectSpec {
  def setupCount: Int
  def mainCount: Int
  def scheduledCount: Int
  def runnableCount: Int

  "Tracing of Scheduler" must {

    "continue delayed message flow" in {
      eventCheck("setup", expected = setupCount) {
        // ignore events
      }

      eventCheck("main", expected = mainCount) {
        countEventsOf[ActorTold] should be(3)
        countEventsOf[ActorReceived] should be(3)
        countEventsOf[ActorCompleted] should be(3)
        countEventsOf[ScheduledOnce] should be(2)
        countEventsOf[ScheduledStarted] should be(scheduledCount)
        countEventsOf[ScheduledCompleted] should be(scheduledCount)
        countEventsOf[RunnableScheduled] should be(runnableCount)
        countEventsOf[RunnableStarted] should be(runnableCount)
        countEventsOf[RunnableCompleted] should be(runnableCount)

        val completedEvents = eventsOf[ActorCompleted]
        completedEvents.size should be(3)
        // all same trace id
        val traceIds = completedEvents.map(_.trace).toSet
        traceIds.size should be(1)
      }

      eventCheck("poison", expected = 18) {
        // ignore events
      }
    }
  }
}
