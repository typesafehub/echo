/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */

package com.typesafe.trace

import com.typesafe.trace.test.AtmosCollectSpec

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

class Akka22Scala210SchedulerSpec extends Akka22SchedulerSpec
class Akka22Scala211SchedulerSpec extends Akka22SchedulerSpec

abstract class Akka22SchedulerSpec extends SchedulerSpec {
  val setupCount = 20
  val mainCount = 17
  val scheduledCount = 0
  val runnableCount = 2
}

abstract class SchedulerSpec extends AtmosCollectSpec {
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
        countEventsOf[ActorTold] must be(3)
        countEventsOf[ActorReceived] must be(3)
        countEventsOf[ActorCompleted] must be(3)
        countEventsOf[ScheduledOnce] must be(2)
        countEventsOf[ScheduledStarted] must be(scheduledCount)
        countEventsOf[ScheduledCompleted] must be(scheduledCount)
        countEventsOf[RunnableScheduled] must be(runnableCount)
        countEventsOf[RunnableStarted] must be(runnableCount)
        countEventsOf[RunnableCompleted] must be(runnableCount)

        val completedEvents = eventsOf[ActorCompleted]
        completedEvents.size must be(3)
        // all same trace id
        val traceIds = completedEvents.map(_.trace).toSet
        traceIds.size must be(1)
      }

      eventCheck("poison", expected = 18) {
        // ignore events
      }
    }
  }
}
