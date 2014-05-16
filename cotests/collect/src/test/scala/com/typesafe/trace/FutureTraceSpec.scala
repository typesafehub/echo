/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */

package com.typesafe.trace

import com.typesafe.trace.test.EchoCollectSpec

class Akka20FutureTraceSpec extends FutureTraceSpec {
  val createCount = 54
  val awaitedEventCount = 20
  val awaitedFutureCallbackAddedCount = 1
  val awaitedFutureCallbackStartedCount = 1
  val awaitedFutureCallbackCompletedCount = 1
  val outsideActorsFutureEventCount = 6
  val insideActorsFutureEventCount = 12
  val scheduledWrapperEvents = true
}

class Akka21FutureTraceSpec extends FutureTraceSpec {
  val createCount = 20
  val awaitedEventCount = 23
  val awaitedFutureCallbackAddedCount = 2
  val awaitedFutureCallbackStartedCount = 2
  val awaitedFutureCallbackCompletedCount = 2
  val outsideActorsFutureEventCount = 9
  val insideActorsFutureEventCount = 15
  val scheduledWrapperEvents = true
}

class Akka22FutureTraceSpec extends FutureTraceSpec {
  val createCount = 20
  val scheduledWrapperEvents = false
  val awaitedEventCount = 23
  val awaitedFutureCallbackAddedCount = 2
  val awaitedFutureCallbackStartedCount = 2
  val awaitedFutureCallbackCompletedCount = 2
  val outsideActorsFutureEventCount = 9
  val insideActorsFutureEventCount = 15
}

abstract class Akka23FutureTraceSpec extends FutureTraceSpec {
  val createCount = 20
  val scheduledWrapperEvents = false
}

class Akka23Scala210FutureTraceSpec extends Akka23FutureTraceSpec {
  val awaitedEventCount = 23
  val awaitedFutureCallbackAddedCount = 2
  val awaitedFutureCallbackStartedCount = 2
  val awaitedFutureCallbackCompletedCount = 2
  val outsideActorsFutureEventCount = 9
  val insideActorsFutureEventCount = 15
}

class Akka23Scala211FutureTraceSpec extends Akka23FutureTraceSpec {
  val awaitedEventCount = 20
  val awaitedFutureCallbackAddedCount = 1
  val awaitedFutureCallbackStartedCount = 1
  val awaitedFutureCallbackCompletedCount = 1
  val outsideActorsFutureEventCount = 6
  val insideActorsFutureEventCount = 12
}

abstract class FutureTraceSpec extends EchoCollectSpec {

  // set this to true to activate pretty print of traces
  override val printTracesAfterWaitForEvents = false

  def createCount: Int
  def awaitedEventCount: Int
  def scheduledWrapperEvents: Boolean
  def awaitedFutureCallbackAddedCount: Int
  def awaitedFutureCallbackStartedCount: Int
  def awaitedFutureCallbackCompletedCount: Int
  def outsideActorsFutureEventCount: Int
  def insideActorsFutureEventCount: Int

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
      eventCheck(expected = awaitedEventCount) {
        countEventsOf[ActorAsked] should be(1)
        countEventsOf[ActorTold] should be(3)
        countEventsOf[ActorReceived] should be(3)
        countEventsOf[ActorCompleted] should be(3)

        countEventsOf[FutureCreated] should be(1)
        countEventsOf[FutureCallbackAdded] should be(awaitedFutureCallbackAddedCount)
        countEventsOf[FutureCallbackStarted] should be(awaitedFutureCallbackStartedCount)
        countEventsOf[FutureCallbackCompleted] should be(awaitedFutureCallbackCompletedCount)
        countEventsOf[FutureSucceeded] should be(1)
        countEventsOf[FutureAwaited] should be(1)

        countEventsOf[TempActorCreated] should be(1)
        countEventsOf[TempActorStopped] should be(1)

        countEventsOf[ScheduledOnce] should be(1)
        countEventsOf[ScheduledCancelled] should be(1)

        // verify correct FutureInfo
        val created = annotationsOf[FutureCreated].head
        val completed = annotationsOf[FutureSucceeded].head
        val awaited = annotationsOf[FutureAwaited].head
        created.info shouldEqual completed.info
        created.info shouldEqual awaited.info
      }
    }

    "trace actors that ask blocking with await after reply" in {
      eventCheck(expected = 20) {
        countEventsOf[ActorAsked] should be(1)
        countEventsOf[ActorTold] should be(3)
        countEventsOf[ActorReceived] should be(3)
        countEventsOf[ActorCompleted] should be(3)

        countEventsOf[FutureCreated] should be(1)
        countEventsOf[FutureCallbackAdded] should be(1)
        countEventsOf[FutureCallbackStarted] should be(1)
        countEventsOf[FutureCallbackCompleted] should be(1)
        countEventsOf[FutureSucceeded] should be(1)
        countEventsOf[FutureAwaited] should be(1)

        countEventsOf[TempActorCreated] should be(1)
        countEventsOf[TempActorStopped] should be(1)

        countEventsOf[ScheduledOnce] should be(1)
        countEventsOf[ScheduledCancelled] should be(1)

        // verify correct FutureInfo
        val created = annotationsOf[FutureCreated].head
        val completed = annotationsOf[FutureSucceeded].head
        val awaited = annotationsOf[FutureAwaited].head
        created.info shouldEqual completed.info
        created.info shouldEqual awaited.info
      }
    }

    "trace actors that ask blocking and receiver times out" in {
      eventCheck(expected = awaitedEventCount) {
        countEventsOf[ActorAsked] should be(1)
        countEventsOf[ActorTold] should be(3)
        countEventsOf[ActorReceived] should be(3)
        countEventsOf[ActorCompleted] should be(3)

        countEventsOf[FutureCreated] should be(1)
        countEventsOf[FutureCallbackAdded] should be(awaitedFutureCallbackAddedCount)
        countEventsOf[FutureCallbackStarted] should be(awaitedFutureCallbackStartedCount)
        countEventsOf[FutureCallbackCompleted] should be(awaitedFutureCallbackCompletedCount)
        countEventsOf[FutureSucceeded] should be(1)
        countEventsOf[FutureAwaitTimedOut] should be(1)

        countEventsOf[TempActorCreated] should be(1)
        countEventsOf[TempActorStopped] should be(1)

        countEventsOf[ScheduledOnce] should be(1)
        countEventsOf[ScheduledCancelled] should be(1)

        // verify correct FutureInfo
        val created = annotationsOf[FutureCreated].head
        val completed = annotationsOf[FutureSucceeded].head
        val timedOut = annotationsOf[FutureAwaitTimedOut].head
        created.info shouldEqual completed.info
        created.info shouldEqual timedOut.info
      }
    }

    "trace actors that ask blocking and receiver replies with exception" in {
      eventCheck(expected = awaitedEventCount) {
        countEventsOf[ActorAsked] should be(1)
        countEventsOf[ActorTold] should be(3)
        countEventsOf[ActorReceived] should be(3)
        countEventsOf[ActorCompleted] should be(3)

        countEventsOf[FutureCreated] should be(1)
        countEventsOf[FutureCallbackAdded] should be(awaitedFutureCallbackAddedCount)
        countEventsOf[FutureCallbackStarted] should be(awaitedFutureCallbackStartedCount)
        countEventsOf[FutureCallbackCompleted] should be(awaitedFutureCallbackCompletedCount)
        countEventsOf[FutureFailed] should be(1)
        countEventsOf[FutureAwaited] should be(1)

        countEventsOf[TempActorCreated] should be(1)
        countEventsOf[TempActorStopped] should be(1)

        countEventsOf[ScheduledOnce] should be(1)
        countEventsOf[ScheduledCancelled] should be(1)

        // verify correct FutureInfo
        val created = annotationsOf[FutureCreated].head
        val failed = annotationsOf[FutureFailed].head
        val awaited = annotationsOf[FutureAwaited].head
        created.info shouldEqual failed.info
        created.info shouldEqual awaited.info
        failed.exception should startWith("com.typesafe.trace.util.ExpectedFailureException")
      }
    }

    "trace actors that ask with callback" in {
      eventCheck(expected = 28) {
        countEventsOf[ActorAsked] should be(1)
        countEventsOf[ActorTold] should be(4)
        countEventsOf[ActorReceived] should be(4)
        countEventsOf[ActorCompleted] should be(4)

        countEventsOf[FutureCreated] should be(1)
        countEventsOf[FutureCallbackAdded] should be(3)
        countEventsOf[FutureCallbackStarted] should be(3)
        countEventsOf[FutureCallbackCompleted] should be(3)
        countEventsOf[FutureSucceeded] should be(1)

        countEventsOf[TempActorCreated] should be(1)
        countEventsOf[TempActorStopped] should be(1)

        countEventsOf[ScheduledOnce] should be(1)
        countEventsOf[ScheduledCancelled] should be(1)

        assertTraceContinueInCallback()

        // verify correct FutureInfo
        val created = annotationsOf[FutureCreated].head
        val completed = annotationsOf[FutureSucceeded].head
        val callbackAdded = annotationsOf[FutureCallbackAdded]
        val callbackStarted = annotationsOf[FutureCallbackStarted]
        val callbackCompleted = annotationsOf[FutureCallbackCompleted]
        created.info shouldEqual completed.info
        for (callback ← callbackAdded) created.info shouldEqual callback.info
        for (callback ← callbackStarted) created.info shouldEqual callback.info
        for (callback ← callbackCompleted) created.info shouldEqual callback.info
      }
    }

    "trace actors that ask with callback added after reply" in {
      eventCheck(expected = 28) {
        countEventsOf[ActorAsked] should be(1)
        countEventsOf[ActorTold] should be(4)
        countEventsOf[ActorReceived] should be(4)
        countEventsOf[ActorCompleted] should be(4)

        countEventsOf[FutureCreated] should be(1)
        countEventsOf[FutureCallbackAdded] should be(3)
        countEventsOf[FutureCallbackStarted] should be(3)
        countEventsOf[FutureCallbackCompleted] should be(3)
        countEventsOf[FutureSucceeded] should be(1)

        countEventsOf[TempActorCreated] should be(1)
        countEventsOf[TempActorStopped] should be(1)

        countEventsOf[ScheduledOnce] should be(1)
        countEventsOf[ScheduledCancelled] should be(1)

        assertTraceContinueInCallback()

        // verify correct FutureInfo
        val created = annotationsOf[FutureCreated].head
        val completed = annotationsOf[FutureSucceeded].head
        val callbackAdded = annotationsOf[FutureCallbackAdded]
        val callbackStarted = annotationsOf[FutureCallbackStarted]
        val callbackCompleted = annotationsOf[FutureCallbackCompleted]
        created.info shouldEqual completed.info
        for (callback ← callbackAdded) created.info shouldEqual callback.info
        for (callback ← callbackStarted) created.info shouldEqual callback.info
        for (callback ← callbackCompleted) created.info shouldEqual callback.info
      }
    }

    "trace actors that ask with callback and receiver replies with exception" in {
      eventCheck(expected = 28) {
        countEventsOf[ActorAsked] should be(1)
        countEventsOf[ActorTold] should be(4)
        countEventsOf[ActorReceived] should be(4)
        countEventsOf[ActorCompleted] should be(4)

        countEventsOf[FutureCreated] should be(1)
        countEventsOf[FutureCallbackAdded] should be(3)
        countEventsOf[FutureCallbackStarted] should be(3)
        countEventsOf[FutureCallbackCompleted] should be(3)
        countEventsOf[FutureFailed] should be(1)

        countEventsOf[TempActorCreated] should be(1)
        countEventsOf[TempActorStopped] should be(1)

        countEventsOf[ScheduledOnce] should be(1)
        countEventsOf[ScheduledCancelled] should be(1)

        assertTraceContinueInCallback(exception = true)

        // verify correct FutureInfo
        val created = annotationsOf[FutureCreated].head
        val failed = annotationsOf[FutureFailed].head
        val callbackAdded = annotationsOf[FutureCallbackAdded]
        val callbackStarted = annotationsOf[FutureCallbackStarted]
        val callbackCompleted = annotationsOf[FutureCallbackCompleted]
        created.info shouldEqual failed.info
        failed.exception should startWith("com.typesafe.trace.util.ExpectedFailureException")
        for (callback ← callbackAdded) created.info shouldEqual callback.info
        for (callback ← callbackStarted) created.info shouldEqual callback.info
        for (callback ← callbackCompleted) created.info shouldEqual callback.info
      }
    }

    "trace actors that ask with callback added after reply and receiver replies with exception" in {
      eventCheck(expected = 28) {
        countEventsOf[ActorAsked] should be(1)
        countEventsOf[ActorTold] should be(4)
        countEventsOf[ActorReceived] should be(4)
        countEventsOf[ActorCompleted] should be(4)

        countEventsOf[FutureCreated] should be(1)
        countEventsOf[FutureCallbackAdded] should be(3)
        countEventsOf[FutureCallbackStarted] should be(3)
        countEventsOf[FutureCallbackCompleted] should be(3)
        countEventsOf[FutureFailed] should be(1)

        countEventsOf[TempActorCreated] should be(1)
        countEventsOf[TempActorStopped] should be(1)

        countEventsOf[ScheduledOnce] should be(1)
        countEventsOf[ScheduledCancelled] should be(1)

        assertTraceContinueInCallback(exception = true)

        // verify correct FutureInfo
        val created = annotationsOf[FutureCreated].head
        val failed = annotationsOf[FutureFailed].head
        val callbackAdded = annotationsOf[FutureCallbackAdded]
        val callbackStarted = annotationsOf[FutureCallbackStarted]
        val callbackCompleted = annotationsOf[FutureCallbackCompleted]
        created.info shouldEqual failed.info
        failed.exception should startWith("com.typesafe.trace.util.ExpectedFailureException")
        for (callback ← callbackAdded) created.info shouldEqual callback.info
        for (callback ← callbackStarted) created.info shouldEqual callback.info
        for (callback ← callbackCompleted) created.info shouldEqual callback.info
      }
    }

    "trace actors that ask with callback and receiver times out" in {
      eventCheck(expected = if (scheduledWrapperEvents) 36 else 34) {
        countEventsOf[ActorAsked] should be(1)
        countEventsOf[ActorTold] should be(4)
        countEventsOf[ActorReceived] should be(4)
        countEventsOf[ActorCompleted] should be(4)

        countEventsOf[FutureCreated] should be(1)
        countEventsOf[FutureCallbackAdded] should be(4)
        countEventsOf[FutureCallbackStarted] should be(4)
        countEventsOf[FutureCallbackCompleted] should be(4)
        countEventsOf[FutureFailed] should be(1)

        countEventsOf[TempActorCreated] should be(1)
        countEventsOf[TempActorStopped] should be(1)

        countEventsOf[ScheduledOnce] should be(1)

        if (scheduledWrapperEvents) {
          countEventsOf[ScheduledStarted] should be(1)
          countEventsOf[ScheduledCompleted] should be(1)
        }

        countEventsOf[RunnableScheduled] should be(1)
        countEventsOf[RunnableStarted] should be(1)
        countEventsOf[RunnableCompleted] should be(1)

        assertTraceContinueInCallback(timeout = true)

        // verify correct FutureInfo
        val created = annotationsOf[FutureCreated].head
        val failed = annotationsOf[FutureFailed].head
        val callbackAdded = annotationsOf[FutureCallbackAdded]
        val callbackStarted = annotationsOf[FutureCallbackStarted]
        val callbackCompleted = annotationsOf[FutureCallbackCompleted]
        created.info shouldEqual failed.info
        failed.exception should startWith("akka.pattern.AskTimeoutException")
        for (callback ← callbackAdded) created.info shouldEqual callback.info
        for (callback ← callbackStarted) created.info shouldEqual callback.info
        for (callback ← callbackCompleted) created.info shouldEqual callback.info
      }
    }

    "trace futures that are used outside actors" in {
      eventCheck(expected = outsideActorsFutureEventCount) {
        countEventsOf[FutureCreated] should be(1)
        countEventsOf[FutureScheduled] should be(1)
        countEventsOf[FutureSucceeded] should be(1)
        countEventsOf[FutureAwaited] should be(1)

        countEventsOf[RunnableStarted] should be(1)
        countEventsOf[RunnableCompleted] should be(1)

        // verify correct FutureInfo
        val created = annotationsOf[FutureCreated].head
        val scheduled = annotationsOf[FutureScheduled].head
        val completed = annotationsOf[FutureSucceeded].head
        val awaited = annotationsOf[FutureAwaited].head
        created.info shouldEqual completed.info
        created.info shouldEqual scheduled.info
        created.info shouldEqual awaited.info
      }
    }

    "trace futures that are used inside actor" in {
      eventCheck(expected = insideActorsFutureEventCount) {
        countEventsOf[ActorTold] should be(2)
        countEventsOf[ActorReceived] should be(2)
        countEventsOf[ActorCompleted] should be(2)

        countEventsOf[FutureCreated] should be(1)
        countEventsOf[FutureScheduled] should be(1)
        countEventsOf[FutureSucceeded] should be(1)
        countEventsOf[FutureAwaited] should be(1)

        countEventsOf[RunnableStarted] should be(1)
        countEventsOf[RunnableCompleted] should be(1)

        // verify correct FutureInfo
        val created = annotationsOf[FutureCreated].head
        val scheduled = annotationsOf[FutureScheduled].head
        val completed = annotationsOf[FutureSucceeded].head
        val awaited = annotationsOf[FutureAwaited].head
        created.info shouldEqual completed.info
        created.info shouldEqual scheduled.info
        created.info shouldEqual awaited.info
      }
    }

    "trace kept promises" in {
      eventCheck(expected = 10) {
        countEventsOf[FutureCreated] should be(2)
        countEventsOf[FutureSucceeded] should be(1)
        countEventsOf[FutureFailed] should be(1)
        countEventsOf[FutureCallbackAdded] should be(2)
        countEventsOf[FutureCallbackStarted] should be(2)
        countEventsOf[FutureCallbackCompleted] should be(2)
      }
    }

    "record trace info of future results" in {
      eventCheck(expected = 16) {
        countEventsOf[ActorAsked] should be(1)
        countEventsOf[ActorTold] should be(2)
        countEventsOf[ActorReceived] should be(2)
        countEventsOf[ActorCompleted] should be(2)

        countEventsOf[FutureCreated] should be(1)
        countEventsOf[FutureCallbackAdded] should be(1)
        countEventsOf[FutureCallbackStarted] should be(1)
        countEventsOf[FutureCallbackCompleted] should be(1)
        countEventsOf[FutureSucceeded] should be(1)

        countEventsOf[TempActorCreated] should be(1)
        countEventsOf[TempActorStopped] should be(1)

        countEventsOf[ScheduledOnce] should be(1)
        countEventsOf[ScheduledCancelled] should be(1)

        // verify future result info matches actor info
        val asked = annotationsOf[ActorAsked].head
        val succeeded = annotationsOf[FutureSucceeded].head
        asked.info shouldEqual succeeded.resultInfo
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

    first.isEmpty should be(false)

    val afterCallback = eventsOf[ActorTold].find(
      _.annotation.asInstanceOf[ActorTold].message startsWith finalMessage)

    afterCallback should not be (None)
    afterCallback.get.trace should be(first.head.trace)
  }
}
