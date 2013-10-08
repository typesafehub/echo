/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */

package com.typesafe.trace

import com.typesafe.trace.test.EchoCollectSpec

class Akka20FutureTraceSpec extends FutureTraceSpec {
  val createCount = 54
  val scheduledWrapperEvents = true
}

class Akka21FutureTraceSpec extends FutureTraceSpec {
  val createCount = 20
  val scheduledWrapperEvents = true
}

class Akka22Scala210FutureTraceSpec extends Akka22FutureTraceSpec
class Akka22Scala211FutureTraceSpec extends Akka22FutureTraceSpec

abstract class Akka22FutureTraceSpec extends FutureTraceSpec {
  val createCount = 20
  val scheduledWrapperEvents = false
}

abstract class FutureTraceSpec extends EchoCollectSpec {

  // set this to true to activate pretty print of traces
  override val printTracesAfterWaitForEvents = false

  def createCount: Int
  def scheduledWrapperEvents: Boolean

  override def beforeEach() = {
    super.beforeEach()
    barrier("create-actors")
    eventCheck("create-actors", expected = createCount) {}
  }

  override def afterEach() = {
    barrier("poison")
    eventCheck("poison", expected = 18) {}
    super.afterEach()
  }

  "FutureTrace" must {

    "trace actors that ask blocking" in {
      eventCheck(expected = 20) {
        countEventsOf[ActorAsked] must be(1)
        countEventsOf[ActorTold] must be(3)
        countEventsOf[ActorReceived] must be(3)
        countEventsOf[ActorCompleted] must be(3)

        countEventsOf[FutureCreated] must be(1)
        countEventsOf[FutureCallbackAdded] must be(1)
        countEventsOf[FutureCallbackStarted] must be(1)
        countEventsOf[FutureCallbackCompleted] must be(1)
        countEventsOf[FutureSucceeded] must be(1)
        countEventsOf[FutureAwaited] must be(1)

        countEventsOf[TempActorCreated] must be(1)
        countEventsOf[TempActorStopped] must be(1)

        countEventsOf[ScheduledOnce] must be(1)
        countEventsOf[ScheduledCancelled] must be(1)

        // verify correct FutureInfo
        val created = annotationsOf[FutureCreated].head
        val completed = annotationsOf[FutureSucceeded].head
        val awaited = annotationsOf[FutureAwaited].head
        created.info must be === completed.info
        created.info must be === awaited.info
      }
    }

    "trace actors that ask blocking with await after reply" in {
      eventCheck(expected = 20) {
        countEventsOf[ActorAsked] must be(1)
        countEventsOf[ActorTold] must be(3)
        countEventsOf[ActorReceived] must be(3)
        countEventsOf[ActorCompleted] must be(3)

        countEventsOf[FutureCreated] must be(1)
        countEventsOf[FutureCallbackAdded] must be(1)
        countEventsOf[FutureCallbackStarted] must be(1)
        countEventsOf[FutureCallbackCompleted] must be(1)
        countEventsOf[FutureSucceeded] must be(1)
        countEventsOf[FutureAwaited] must be(1)

        countEventsOf[TempActorCreated] must be(1)
        countEventsOf[TempActorStopped] must be(1)

        countEventsOf[ScheduledOnce] must be(1)
        countEventsOf[ScheduledCancelled] must be(1)

        // verify correct FutureInfo
        val created = annotationsOf[FutureCreated].head
        val completed = annotationsOf[FutureSucceeded].head
        val awaited = annotationsOf[FutureAwaited].head
        created.info must be === completed.info
        created.info must be === awaited.info
      }
    }

    "trace actors that ask blocking and receiver times out" in {
      eventCheck(expected = 20) {
        countEventsOf[ActorAsked] must be(1)
        countEventsOf[ActorTold] must be(3)
        countEventsOf[ActorReceived] must be(3)
        countEventsOf[ActorCompleted] must be(3)

        countEventsOf[FutureCreated] must be(1)
        countEventsOf[FutureCallbackAdded] must be(1)
        countEventsOf[FutureCallbackStarted] must be(1)
        countEventsOf[FutureCallbackCompleted] must be(1)
        countEventsOf[FutureSucceeded] must be(1)
        countEventsOf[FutureAwaitTimedOut] must be(1)

        countEventsOf[TempActorCreated] must be(1)
        countEventsOf[TempActorStopped] must be(1)

        countEventsOf[ScheduledOnce] must be(1)
        countEventsOf[ScheduledCancelled] must be(1)

        // verify correct FutureInfo
        val created = annotationsOf[FutureCreated].head
        val completed = annotationsOf[FutureSucceeded].head
        val timedOut = annotationsOf[FutureAwaitTimedOut].head
        created.info must be === completed.info
        created.info must be === timedOut.info
      }
    }

    "trace actors that ask blocking and receiver replies with exception" in {
      eventCheck(expected = 20) {
        countEventsOf[ActorAsked] must be(1)
        countEventsOf[ActorTold] must be(3)
        countEventsOf[ActorReceived] must be(3)
        countEventsOf[ActorCompleted] must be(3)

        countEventsOf[FutureCreated] must be(1)
        countEventsOf[FutureCallbackAdded] must be(1)
        countEventsOf[FutureCallbackStarted] must be(1)
        countEventsOf[FutureCallbackCompleted] must be(1)
        countEventsOf[FutureFailed] must be(1)
        countEventsOf[FutureAwaited] must be(1)

        countEventsOf[TempActorCreated] must be(1)
        countEventsOf[TempActorStopped] must be(1)

        countEventsOf[ScheduledOnce] must be(1)
        countEventsOf[ScheduledCancelled] must be(1)

        // verify correct FutureInfo
        val created = annotationsOf[FutureCreated].head
        val failed = annotationsOf[FutureFailed].head
        val awaited = annotationsOf[FutureAwaited].head
        created.info must be === failed.info
        created.info must be === awaited.info
        failed.exception must startWith("com.typesafe.trace.util.ExpectedFailureException")
      }
    }

    "trace actors that ask with callback" in {
      eventCheck(expected = 28) {
        countEventsOf[ActorAsked] must be(1)
        countEventsOf[ActorTold] must be(4)
        countEventsOf[ActorReceived] must be(4)
        countEventsOf[ActorCompleted] must be(4)

        countEventsOf[FutureCreated] must be(1)
        countEventsOf[FutureCallbackAdded] must be(3)
        countEventsOf[FutureCallbackStarted] must be(3)
        countEventsOf[FutureCallbackCompleted] must be(3)
        countEventsOf[FutureSucceeded] must be(1)

        countEventsOf[TempActorCreated] must be(1)
        countEventsOf[TempActorStopped] must be(1)

        countEventsOf[ScheduledOnce] must be(1)
        countEventsOf[ScheduledCancelled] must be(1)

        assertTraceContinueInCallback()

        // verify correct FutureInfo
        val created = annotationsOf[FutureCreated].head
        val completed = annotationsOf[FutureSucceeded].head
        val callbackAdded = annotationsOf[FutureCallbackAdded]
        val callbackStarted = annotationsOf[FutureCallbackStarted]
        val callbackCompleted = annotationsOf[FutureCallbackCompleted]
        created.info must be === completed.info
        for (callback ← callbackAdded) created.info must be === callback.info
        for (callback ← callbackStarted) created.info must be === callback.info
        for (callback ← callbackCompleted) created.info must be === callback.info
      }
    }

    "trace actors that ask with callback added after reply" in {
      eventCheck(expected = 28) {
        countEventsOf[ActorAsked] must be(1)
        countEventsOf[ActorTold] must be(4)
        countEventsOf[ActorReceived] must be(4)
        countEventsOf[ActorCompleted] must be(4)

        countEventsOf[FutureCreated] must be(1)
        countEventsOf[FutureCallbackAdded] must be(3)
        countEventsOf[FutureCallbackStarted] must be(3)
        countEventsOf[FutureCallbackCompleted] must be(3)
        countEventsOf[FutureSucceeded] must be(1)

        countEventsOf[TempActorCreated] must be(1)
        countEventsOf[TempActorStopped] must be(1)

        countEventsOf[ScheduledOnce] must be(1)
        countEventsOf[ScheduledCancelled] must be(1)

        assertTraceContinueInCallback()

        // verify correct FutureInfo
        val created = annotationsOf[FutureCreated].head
        val completed = annotationsOf[FutureSucceeded].head
        val callbackAdded = annotationsOf[FutureCallbackAdded]
        val callbackStarted = annotationsOf[FutureCallbackStarted]
        val callbackCompleted = annotationsOf[FutureCallbackCompleted]
        created.info must be === completed.info
        for (callback ← callbackAdded) created.info must be === callback.info
        for (callback ← callbackStarted) created.info must be === callback.info
        for (callback ← callbackCompleted) created.info must be === callback.info
      }
    }

    "trace actors that ask with callback and receiver replies with exception" in {
      eventCheck(expected = 28) {
        countEventsOf[ActorAsked] must be(1)
        countEventsOf[ActorTold] must be(4)
        countEventsOf[ActorReceived] must be(4)
        countEventsOf[ActorCompleted] must be(4)

        countEventsOf[FutureCreated] must be(1)
        countEventsOf[FutureCallbackAdded] must be(3)
        countEventsOf[FutureCallbackStarted] must be(3)
        countEventsOf[FutureCallbackCompleted] must be(3)
        countEventsOf[FutureFailed] must be(1)

        countEventsOf[TempActorCreated] must be(1)
        countEventsOf[TempActorStopped] must be(1)

        countEventsOf[ScheduledOnce] must be(1)
        countEventsOf[ScheduledCancelled] must be(1)

        assertTraceContinueInCallback(exception = true)

        // verify correct FutureInfo
        val created = annotationsOf[FutureCreated].head
        val failed = annotationsOf[FutureFailed].head
        val callbackAdded = annotationsOf[FutureCallbackAdded]
        val callbackStarted = annotationsOf[FutureCallbackStarted]
        val callbackCompleted = annotationsOf[FutureCallbackCompleted]
        created.info must be === failed.info
        failed.exception must startWith("com.typesafe.trace.util.ExpectedFailureException")
        for (callback ← callbackAdded) created.info must be === callback.info
        for (callback ← callbackStarted) created.info must be === callback.info
        for (callback ← callbackCompleted) created.info must be === callback.info
      }
    }

    "trace actors that ask with callback added after reply and receiver replies with exception" in {
      eventCheck(expected = 28) {
        countEventsOf[ActorAsked] must be(1)
        countEventsOf[ActorTold] must be(4)
        countEventsOf[ActorReceived] must be(4)
        countEventsOf[ActorCompleted] must be(4)

        countEventsOf[FutureCreated] must be(1)
        countEventsOf[FutureCallbackAdded] must be(3)
        countEventsOf[FutureCallbackStarted] must be(3)
        countEventsOf[FutureCallbackCompleted] must be(3)
        countEventsOf[FutureFailed] must be(1)

        countEventsOf[TempActorCreated] must be(1)
        countEventsOf[TempActorStopped] must be(1)

        countEventsOf[ScheduledOnce] must be(1)
        countEventsOf[ScheduledCancelled] must be(1)

        assertTraceContinueInCallback(exception = true)

        // verify correct FutureInfo
        val created = annotationsOf[FutureCreated].head
        val failed = annotationsOf[FutureFailed].head
        val callbackAdded = annotationsOf[FutureCallbackAdded]
        val callbackStarted = annotationsOf[FutureCallbackStarted]
        val callbackCompleted = annotationsOf[FutureCallbackCompleted]
        created.info must be === failed.info
        failed.exception must startWith("com.typesafe.trace.util.ExpectedFailureException")
        for (callback ← callbackAdded) created.info must be === callback.info
        for (callback ← callbackStarted) created.info must be === callback.info
        for (callback ← callbackCompleted) created.info must be === callback.info
      }
    }

    "trace actors that ask with callback and receiver times out" in {
      eventCheck(expected = if (scheduledWrapperEvents) 36 else 34) {
        countEventsOf[ActorAsked] must be(1)
        countEventsOf[ActorTold] must be(4)
        countEventsOf[ActorReceived] must be(4)
        countEventsOf[ActorCompleted] must be(4)

        countEventsOf[FutureCreated] must be(1)
        countEventsOf[FutureCallbackAdded] must be(4)
        countEventsOf[FutureCallbackStarted] must be(4)
        countEventsOf[FutureCallbackCompleted] must be(4)
        countEventsOf[FutureFailed] must be(1)

        countEventsOf[TempActorCreated] must be(1)
        countEventsOf[TempActorStopped] must be(1)

        countEventsOf[ScheduledOnce] must be(1)

        if (scheduledWrapperEvents) {
          countEventsOf[ScheduledStarted] must be(1)
          countEventsOf[ScheduledCompleted] must be(1)
        }

        countEventsOf[RunnableScheduled] must be(1)
        countEventsOf[RunnableStarted] must be(1)
        countEventsOf[RunnableCompleted] must be(1)

        assertTraceContinueInCallback(timeout = true)

        // verify correct FutureInfo
        val created = annotationsOf[FutureCreated].head
        val failed = annotationsOf[FutureFailed].head
        val callbackAdded = annotationsOf[FutureCallbackAdded]
        val callbackStarted = annotationsOf[FutureCallbackStarted]
        val callbackCompleted = annotationsOf[FutureCallbackCompleted]
        created.info must be === failed.info
        failed.exception must startWith("akka.pattern.AskTimeoutException")
        for (callback ← callbackAdded) created.info must be === callback.info
        for (callback ← callbackStarted) created.info must be === callback.info
        for (callback ← callbackCompleted) created.info must be === callback.info
      }
    }

    "trace futures that are used outside actors" in {
      eventCheck(expected = 6) {
        countEventsOf[FutureCreated] must be(1)
        countEventsOf[FutureScheduled] must be(1)
        countEventsOf[FutureSucceeded] must be(1)
        countEventsOf[FutureAwaited] must be(1)

        countEventsOf[RunnableStarted] must be(1)
        countEventsOf[RunnableCompleted] must be(1)

        // verify correct FutureInfo
        val created = annotationsOf[FutureCreated].head
        val scheduled = annotationsOf[FutureScheduled].head
        val completed = annotationsOf[FutureSucceeded].head
        val awaited = annotationsOf[FutureAwaited].head
        created.info must be === completed.info
        created.info must be === scheduled.info
        created.info must be === awaited.info
      }
    }

    "trace futures that are used inside actor" in {
      eventCheck(expected = 12) {
        countEventsOf[ActorTold] must be(2)
        countEventsOf[ActorReceived] must be(2)
        countEventsOf[ActorCompleted] must be(2)

        countEventsOf[FutureCreated] must be(1)
        countEventsOf[FutureScheduled] must be(1)
        countEventsOf[FutureSucceeded] must be(1)
        countEventsOf[FutureAwaited] must be(1)

        countEventsOf[RunnableStarted] must be(1)
        countEventsOf[RunnableCompleted] must be(1)

        // verify correct FutureInfo
        val created = annotationsOf[FutureCreated].head
        val scheduled = annotationsOf[FutureScheduled].head
        val completed = annotationsOf[FutureSucceeded].head
        val awaited = annotationsOf[FutureAwaited].head
        created.info must be === completed.info
        created.info must be === scheduled.info
        created.info must be === awaited.info
      }
    }

    "trace kept promises" in {
      eventCheck(expected = 10) {
        countEventsOf[FutureCreated] must be(2)
        countEventsOf[FutureSucceeded] must be(1)
        countEventsOf[FutureFailed] must be(1)
        countEventsOf[FutureCallbackAdded] must be(2)
        countEventsOf[FutureCallbackStarted] must be(2)
        countEventsOf[FutureCallbackCompleted] must be(2)
      }
    }

    "record trace info of future results" in {
      eventCheck(expected = 16) {
        countEventsOf[ActorAsked] must be(1)
        countEventsOf[ActorTold] must be(2)
        countEventsOf[ActorReceived] must be(2)
        countEventsOf[ActorCompleted] must be(2)

        countEventsOf[FutureCreated] must be(1)
        countEventsOf[FutureCallbackAdded] must be(1)
        countEventsOf[FutureCallbackStarted] must be(1)
        countEventsOf[FutureCallbackCompleted] must be(1)
        countEventsOf[FutureSucceeded] must be(1)

        countEventsOf[TempActorCreated] must be(1)
        countEventsOf[TempActorStopped] must be(1)

        countEventsOf[ScheduledOnce] must be(1)
        countEventsOf[ScheduledCancelled] must be(1)

        // verify future result info matches actor info
        val asked = annotationsOf[ActorAsked].head
        val succeeded = annotationsOf[FutureSucceeded].head
        asked.info must be === succeeded.resultInfo
      }
    }

  }

  def assertTraceContinueInCallback(exception: Boolean = false, timeout: Boolean = false) {
    val finalMessage =
      if (exception) "ExceptionFromCallback"
      else if (timeout) "TimeoutFromCallback"
      else "ResultFromCallback"

    val first = eventsOf[ActorTold].filter(event ⇒
      event.annotation.asInstanceOf[ActorTold].message.startsWith("AskWithCallback") ||
        event.annotation.asInstanceOf[ActorTold].message.startsWith("UseFuturesInsideActor"))

    first.isEmpty must be(false)

    val afterCallback = eventsOf[ActorTold].find(
      _.annotation.asInstanceOf[ActorTold].message startsWith finalMessage)

    afterCallback must not be (None)
    afterCallback.get.trace must be(first.head.trace)
  }
}
