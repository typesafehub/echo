/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */

package com.typesafe.trace

import com.typesafe.trace.test.EchoCollectSpec

// ===============
// Expected events
// ===============
//
// -------------------------
// Ask and reply = 17 events
//
//   1 ActorAsked
//   2 ActorTold
//   2 ActorReceived
//   2 ActorCompleted
//   1 TempActorCreated
//   1 TempActorStopped
//   1 FutureCreated
//   1 FutureSucceeded
//   1 FutureCallbackAdded
//   1 FutureCallbackStarted
//   1 FutureCallbackCompleted
//   1 FutureAwaited
//   1 ScheduledOnce
//   1 ScheduledCancelled
//
//   ActorAsked -> (FutureCreated, TempActorCreated, FutureCallbackAdded -> (FutureCallbackStarted, TempActorStopped, FutureCallbackCompleted),
//                  ActorTold -> (ActorReceived, ActorTold (reply) -> (ActorReceived, FutureSucceeded, ActorCompleted), ActorCompleted))
//   FutureAwaited
//
// -----------------------------------
// Akka 2.0 system.actorOf = 27 events
//
//   (17 events from ask and reply)
//
//   1 TopLevelActorRequested
//   1 TopLevelActorCreated
//   1 ActorRequested
//   1 ActorCreated
//   3 SuperviseSysMsg (dispatched, received, completed)
//   3 CreateSysMsg (disatched, received, completed)
//   1 ActorAsked
//   2 ActorTold
//   2 ActorReceived
//   2 ActorCompleted
//   1 TempActorCreated
//   1 TempActorStopped
//   1 FutureCreated
//   1 FutureSucceeded
//   1 FutureCallbackAdded
//   1 FutureCallbackStarted
//   1 FutureCallbackCompleted
//   1 FutureAwaited
//   1 ScheduledOnce
//   1 ScheduledCancelled
//
// -----------------------------------
// Akka 2.1 system.actorOf = 10 events
//
//   1 TopLevelActorRequested
//   1 TopLevelActorCreated
//   1 ActorRequested
//   1 ActorCreated
//   3 SuperviseSysMsg (dispatched, received, completed)
//   3 CreateSysMsg (disatched, received, completed)
//
// ----------------------
// poison pill = 9 events
//
//   1 ActorTold
//   1 ActorAutoReceived
//   1 ActorAutoCompleted
//   3 TerminateSysMsg (dispatched, received, completed)
//   3 ChildTerminatedSysMsg (dispatched, received, completed)
//
// -------------------------------------
// failing message (recreate) = 9 events
//
//   1 ActorTold
//   1 ActorReceived
//   1 ActorCompleted
//   1 ActorFailed
//   1 ActorAutoReceived
//   1 ActorAutoCompleted
//   3 RecreateSysMsg (dispatched, received, completed)

class Akka20TracingSpec extends EchoCollectSpec {

  "Tracing" must {

    "store all trace events in memory" in {
      eventCheck(expected = 130) {
        countTraces should be(1)

        countEventsOf[GroupStarted] should be(2)
        countEventsOf[GroupEnded] should be(2)

        countEventsOf[Marker] should be(6)

        countEventsOf[TopLevelActorCreated] should be(3)
        countEventsOf[TopLevelActorRequested] should be(3)

        countEventsOf[SysMsgDispatched] should be(12)
        countEventsOf[SysMsgReceived] should be(12)
        countEventsOf[SysMsgCompleted] should be(12)

        countSysMsgEventsOf[SysMsgCompleted](CreateSysMsg) should be(3)
        countSysMsgEventsOf[SysMsgCompleted](TerminateSysMsg) should be(3)

        countSysMsgEventsOf[SysMsgCompleted, SuperviseSysMsg] should be(3)
        countSysMsgEventsOf[SysMsgCompleted, ChildTerminatedSysMsg] should be(3)

        countEventsOf[ActorRequested] should be(3)
        countEventsOf[ActorCreated] should be(3)
        countEventsOf[ActorTold] should be(13)
        countEventsOf[ActorAutoReceived] should be(3)
        countEventsOf[ActorAutoCompleted] should be(3)
        countEventsOf[ActorReceived] should be(10)
        countEventsOf[ActorCompleted] should be(10)
        countEventsOf[ActorAsked] should be(3)
        countEventsOf[TempActorCreated] should be(3)
        countEventsOf[TempActorStopped] should be(3)

        countEventsOf[FutureCreated] should be(3)
        countEventsOf[FutureSucceeded] should be(3)
        countEventsOf[FutureCallbackAdded] should be(3)
        countEventsOf[FutureCallbackStarted] should be(3)
        countEventsOf[FutureCallbackCompleted] should be(3)
        countEventsOf[FutureAwaited] should be(3)

        countEventsOf[ScheduledOnce] should be(3)
        countEventsOf[ScheduledCancelled] should be(3)
      }
    }

    "record actor failed events" in {
      eventCheck(expected = 65) {
        countTraces should be(1)

        countEventsOf[GroupStarted] should be(1)
        countEventsOf[GroupEnded] should be(1)

        countEventsOf[TopLevelActorCreated] should be(1)
        countEventsOf[TopLevelActorRequested] should be(1)

        countEventsOf[SysMsgDispatched] should be(5)
        countEventsOf[SysMsgReceived] should be(5)
        countEventsOf[SysMsgCompleted] should be(5)

        countSysMsgEventsOf[SysMsgCompleted](CreateSysMsg) should be(1)
        countSysMsgEventsOf[SysMsgCompleted](TerminateSysMsg) should be(1)

        countSysMsgEventsOf[SysMsgCompleted, SuperviseSysMsg] should be(1)
        countSysMsgEventsOf[SysMsgCompleted, RecreateSysMsg] should be(1)
        countSysMsgEventsOf[SysMsgCompleted, ChildTerminatedSysMsg] should be(1)

        countEventsOf[ActorRequested] should be(1)
        countEventsOf[ActorCreated] should be(1)
        countEventsOf[ActorTold] should be(6)
        countEventsOf[ActorAutoReceived] should be(2)
        countEventsOf[ActorAutoCompleted] should be(2)
        countEventsOf[ActorReceived] should be(5)
        countEventsOf[ActorCompleted] should be(5)
        countEventsOf[ActorAsked] should be(2)
        countEventsOf[ActorFailed] should be(1)
        countEventsOf[TempActorCreated] should be(2)
        countEventsOf[TempActorStopped] should be(2)

        countEventsOf[FutureCreated] should be(2)
        countEventsOf[FutureSucceeded] should be(2)
        countEventsOf[FutureCallbackAdded] should be(2)
        countEventsOf[FutureCallbackStarted] should be(2)
        countEventsOf[FutureCallbackCompleted] should be(2)
        countEventsOf[FutureAwaited] should be(2)

        countEventsOf[ScheduledOnce] should be(2)
        countEventsOf[ScheduledCancelled] should be(2)

        countEventsOf[EventStreamError] should be(1)
      }
    }
  }
}

class Akka21TracingSpec extends EchoCollectSpec {

  "Tracing" must {

    "store all trace events in memory" in {
      eventCheck(expected = 79) {
        countTraces should be(1)

        countEventsOf[GroupStarted] should be(2)
        countEventsOf[GroupEnded] should be(2)

        countEventsOf[Marker] should be(6)

        countEventsOf[TopLevelActorCreated] should be(3)
        countEventsOf[TopLevelActorRequested] should be(3)

        countEventsOf[SysMsgDispatched] should be(12)
        countEventsOf[SysMsgReceived] should be(12)
        countEventsOf[SysMsgCompleted] should be(12)

        countSysMsgEventsOf[SysMsgCompleted](CreateSysMsg) should be(3)
        countSysMsgEventsOf[SysMsgCompleted](TerminateSysMsg) should be(3)

        countSysMsgEventsOf[SysMsgCompleted, SuperviseSysMsg] should be(3)
        countSysMsgEventsOf[SysMsgCompleted, ChildTerminatedSysMsg] should be(3)

        countEventsOf[ActorRequested] should be(3)
        countEventsOf[ActorCreated] should be(3)
        countEventsOf[ActorTold] should be(7)
        countEventsOf[ActorAutoReceived] should be(3)
        countEventsOf[ActorAutoCompleted] should be(3)
        countEventsOf[ActorReceived] should be(4)
        countEventsOf[ActorCompleted] should be(4)
      }
    }

    "record actor failed events" in {
      eventCheck(expected = 51) {
        countTraces should be(1)

        countEventsOf[GroupStarted] should be(1)
        countEventsOf[GroupEnded] should be(1)

        countEventsOf[TopLevelActorCreated] should be(1)
        countEventsOf[TopLevelActorRequested] should be(1)

        countEventsOf[SysMsgDispatched] should be(5)
        countEventsOf[SysMsgReceived] should be(5)
        countEventsOf[SysMsgCompleted] should be(5)

        countSysMsgEventsOf[SysMsgCompleted](CreateSysMsg) should be(1)
        countSysMsgEventsOf[SysMsgCompleted](TerminateSysMsg) should be(1)

        countSysMsgEventsOf[SysMsgCompleted, SuperviseSysMsg] should be(1)
        countSysMsgEventsOf[SysMsgCompleted, RecreateSysMsg] should be(1)
        countSysMsgEventsOf[SysMsgCompleted, ChildTerminatedSysMsg] should be(1)

        countEventsOf[ActorRequested] should be(1)
        countEventsOf[ActorCreated] should be(1)
        countEventsOf[ActorTold] should be(4)
        countEventsOf[ActorAutoReceived] should be(2)
        countEventsOf[ActorAutoCompleted] should be(2)
        countEventsOf[ActorReceived] should be(3)
        countEventsOf[ActorCompleted] should be(3)
        countEventsOf[ActorAsked] should be(1)
        countEventsOf[ActorFailed] should be(1)
        countEventsOf[TempActorCreated] should be(1)
        countEventsOf[TempActorStopped] should be(1)

        countEventsOf[FutureCreated] should be(1)
        countEventsOf[FutureSucceeded] should be(1)
        countEventsOf[FutureCallbackAdded] should be(2)
        countEventsOf[FutureCallbackStarted] should be(2)
        countEventsOf[FutureCallbackCompleted] should be(2)
        countEventsOf[FutureAwaited] should be(1)

        countEventsOf[ScheduledOnce] should be(1)
        countEventsOf[ScheduledCancelled] should be(1)

        countEventsOf[EventStreamError] should be(1)
      }
    }
  }
}

class Akka22TracingSpec extends EchoCollectSpec {

  val failedEventsCount = 51
  val failedFutureCallbackAddedCount = 2
  val failedFutureCallbackStartedCount = 2
  val failedFutureCallbackCompletedCount = 2

  "Tracing" must {

    "store all trace events in memory" in {
      eventCheck(expected = 79) {
        countTraces should be(1)

        countEventsOf[GroupStarted] should be(2)
        countEventsOf[GroupEnded] should be(2)

        countEventsOf[Marker] should be(6)

        countEventsOf[TopLevelActorCreated] should be(3)
        countEventsOf[TopLevelActorRequested] should be(3)

        countEventsOf[SysMsgDispatched] should be(12)
        countEventsOf[SysMsgReceived] should be(12)
        countEventsOf[SysMsgCompleted] should be(12)

        countSysMsgEventsOf[SysMsgCompleted](CreateSysMsg) should be(3)
        countSysMsgEventsOf[SysMsgCompleted](TerminateSysMsg) should be(3)

        countSysMsgEventsOf[SysMsgCompleted, SuperviseSysMsg] should be(3)
        countSysMsgEventsOf[SysMsgCompleted, DeathWatchSysMsg] should be(3)

        countEventsOf[ActorRequested] should be(3)
        countEventsOf[ActorCreated] should be(3)
        countEventsOf[ActorTold] should be(7)
        countEventsOf[ActorAutoReceived] should be(3)
        countEventsOf[ActorAutoCompleted] should be(3)
        countEventsOf[ActorReceived] should be(4)
        countEventsOf[ActorCompleted] should be(4)
      }
    }

    "record actor failed events" in {
      eventCheck(expected = failedEventsCount) {
        countTraces should be(1)

        countEventsOf[GroupStarted] should be(1)
        countEventsOf[GroupEnded] should be(1)

        countEventsOf[TopLevelActorCreated] should be(1)
        countEventsOf[TopLevelActorRequested] should be(1)

        countEventsOf[SysMsgDispatched] should be(5)
        countEventsOf[SysMsgReceived] should be(6)
        countEventsOf[SysMsgCompleted] should be(6)

        countSysMsgEventsOf[SysMsgCompleted](CreateSysMsg) should be(1)
        countSysMsgEventsOf[SysMsgCompleted](TerminateSysMsg) should be(1)

        countSysMsgEventsOf[SysMsgCompleted, SuperviseSysMsg] should be(1)
        countSysMsgEventsOf[SysMsgCompleted, FailedSysMsg] should be(1)
        countSysMsgEventsOf[SysMsgCompleted, RecreateSysMsg] should be(1)
        countSysMsgEventsOf[SysMsgCompleted, DeathWatchSysMsg] should be(1)

        countEventsOf[ActorRequested] should be(1)
        countEventsOf[ActorCreated] should be(1)
        countEventsOf[ActorTold] should be(4)
        countEventsOf[ActorAutoReceived] should be(1)
        countEventsOf[ActorAutoCompleted] should be(1)
        countEventsOf[ActorReceived] should be(3)
        countEventsOf[ActorCompleted] should be(3)
        countEventsOf[ActorAsked] should be(1)
        countEventsOf[ActorFailed] should be(1)
        countEventsOf[TempActorCreated] should be(1)
        countEventsOf[TempActorStopped] should be(1)

        countEventsOf[FutureCreated] should be(1)
        countEventsOf[FutureSucceeded] should be(1)
        countEventsOf[FutureCallbackAdded] should be(failedFutureCallbackAddedCount)
        countEventsOf[FutureCallbackStarted] should be(failedFutureCallbackStartedCount)
        countEventsOf[FutureCallbackCompleted] should be(failedFutureCallbackCompletedCount)
        countEventsOf[FutureAwaited] should be(1)

        countEventsOf[ScheduledOnce] should be(1)
        countEventsOf[ScheduledCancelled] should be(1)

        countEventsOf[EventStreamError] should be(1)
      }
    }
  }
}

class Akka23Scala210TracingSpec extends Akka23TracingSpec {
  val failedEventsCount = 51
  val failedFutureCallbackAddedCount = 2
  val failedFutureCallbackStartedCount = 2
  val failedFutureCallbackCompletedCount = 2
}
class Akka23Scala211TracingSpec extends Akka23TracingSpec {
  val failedEventsCount = 48
  val failedFutureCallbackAddedCount = 1
  val failedFutureCallbackStartedCount = 1
  val failedFutureCallbackCompletedCount = 1
}

abstract class Akka23TracingSpec extends EchoCollectSpec {

  def failedEventsCount: Int
  def failedFutureCallbackAddedCount: Int
  def failedFutureCallbackStartedCount: Int
  def failedFutureCallbackCompletedCount: Int

  "Tracing" must {

    "store all trace events in memory" in {
      eventCheck(expected = 79) {
        countTraces should be(1)

        countEventsOf[GroupStarted] should be(2)
        countEventsOf[GroupEnded] should be(2)

        countEventsOf[Marker] should be(6)

        countEventsOf[TopLevelActorCreated] should be(3)
        countEventsOf[TopLevelActorRequested] should be(3)

        countEventsOf[SysMsgDispatched] should be(12)
        countEventsOf[SysMsgReceived] should be(12)
        countEventsOf[SysMsgCompleted] should be(12)

        countSysMsgEventsOf[SysMsgCompleted](CreateSysMsg) should be(3)
        countSysMsgEventsOf[SysMsgCompleted](TerminateSysMsg) should be(3)

        countSysMsgEventsOf[SysMsgCompleted, SuperviseSysMsg] should be(3)
        countSysMsgEventsOf[SysMsgCompleted, DeathWatchSysMsg] should be(3)

        countEventsOf[ActorRequested] should be(3)
        countEventsOf[ActorCreated] should be(3)
        countEventsOf[ActorTold] should be(7)
        countEventsOf[ActorAutoReceived] should be(3)
        countEventsOf[ActorAutoCompleted] should be(3)
        countEventsOf[ActorReceived] should be(4)
        countEventsOf[ActorCompleted] should be(4)
      }
    }

    "record actor failed events" in {
      eventCheck(expected = failedEventsCount) {
        countTraces should be(1)

        countEventsOf[GroupStarted] should be(1)
        countEventsOf[GroupEnded] should be(1)

        countEventsOf[TopLevelActorCreated] should be(1)
        countEventsOf[TopLevelActorRequested] should be(1)

        countEventsOf[SysMsgDispatched] should be(5)
        countEventsOf[SysMsgReceived] should be(6)
        countEventsOf[SysMsgCompleted] should be(6)

        countSysMsgEventsOf[SysMsgCompleted](CreateSysMsg) should be(1)
        countSysMsgEventsOf[SysMsgCompleted](TerminateSysMsg) should be(1)

        countSysMsgEventsOf[SysMsgCompleted, SuperviseSysMsg] should be(1)
        countSysMsgEventsOf[SysMsgCompleted, FailedSysMsg] should be(1)
        countSysMsgEventsOf[SysMsgCompleted, RecreateSysMsg] should be(1)
        countSysMsgEventsOf[SysMsgCompleted, DeathWatchSysMsg] should be(1)

        countEventsOf[ActorRequested] should be(1)
        countEventsOf[ActorCreated] should be(1)
        countEventsOf[ActorTold] should be(4)
        countEventsOf[ActorAutoReceived] should be(1)
        countEventsOf[ActorAutoCompleted] should be(1)
        countEventsOf[ActorReceived] should be(3)
        countEventsOf[ActorCompleted] should be(3)
        countEventsOf[ActorAsked] should be(1)
        countEventsOf[ActorFailed] should be(1)
        countEventsOf[TempActorCreated] should be(1)
        countEventsOf[TempActorStopped] should be(1)

        countEventsOf[FutureCreated] should be(1)
        countEventsOf[FutureSucceeded] should be(1)
        countEventsOf[FutureCallbackAdded] should be(failedFutureCallbackAddedCount)
        countEventsOf[FutureCallbackStarted] should be(failedFutureCallbackStartedCount)
        countEventsOf[FutureCallbackCompleted] should be(failedFutureCallbackCompletedCount)
        countEventsOf[FutureAwaited] should be(1)

        countEventsOf[ScheduledOnce] should be(1)
        countEventsOf[ScheduledCancelled] should be(1)

        countEventsOf[EventStreamError] should be(1)
      }
    }
  }
}
