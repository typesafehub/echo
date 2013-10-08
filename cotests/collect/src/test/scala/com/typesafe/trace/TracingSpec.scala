/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */

package com.typesafe.trace

import com.typesafe.trace.test.AtmosCollectSpec

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

class Akka20TracingSpec extends AtmosCollectSpec {

  "Tracing" must {

    "store all trace events in memory" in {
      eventCheck(expected = 130) {
        countTraces must be(1)

        countEventsOf[GroupStarted] must be(2)
        countEventsOf[GroupEnded] must be(2)

        countEventsOf[Marker] must be(6)

        countEventsOf[TopLevelActorCreated] must be(3)
        countEventsOf[TopLevelActorRequested] must be(3)

        countEventsOf[SysMsgDispatched] must be(12)
        countEventsOf[SysMsgReceived] must be(12)
        countEventsOf[SysMsgCompleted] must be(12)

        countSysMsgEventsOf[SysMsgCompleted](CreateSysMsg) must be(3)
        countSysMsgEventsOf[SysMsgCompleted](TerminateSysMsg) must be(3)

        countSysMsgEventsOf[SysMsgCompleted, SuperviseSysMsg] must be(3)
        countSysMsgEventsOf[SysMsgCompleted, ChildTerminatedSysMsg] must be(3)

        countEventsOf[ActorRequested] must be(3)
        countEventsOf[ActorCreated] must be(3)
        countEventsOf[ActorTold] must be(13)
        countEventsOf[ActorAutoReceived] must be(3)
        countEventsOf[ActorAutoCompleted] must be(3)
        countEventsOf[ActorReceived] must be(10)
        countEventsOf[ActorCompleted] must be(10)
        countEventsOf[ActorAsked] must be(3)
        countEventsOf[TempActorCreated] must be(3)
        countEventsOf[TempActorStopped] must be(3)

        countEventsOf[FutureCreated] must be(3)
        countEventsOf[FutureSucceeded] must be(3)
        countEventsOf[FutureCallbackAdded] must be(3)
        countEventsOf[FutureCallbackStarted] must be(3)
        countEventsOf[FutureCallbackCompleted] must be(3)
        countEventsOf[FutureAwaited] must be(3)

        countEventsOf[ScheduledOnce] must be(3)
        countEventsOf[ScheduledCancelled] must be(3)
      }
    }

    "record actor failed events" in {
      eventCheck(expected = 65) {
        countTraces must be(1)

        countEventsOf[GroupStarted] must be(1)
        countEventsOf[GroupEnded] must be(1)

        countEventsOf[TopLevelActorCreated] must be(1)
        countEventsOf[TopLevelActorRequested] must be(1)

        countEventsOf[SysMsgDispatched] must be(5)
        countEventsOf[SysMsgReceived] must be(5)
        countEventsOf[SysMsgCompleted] must be(5)

        countSysMsgEventsOf[SysMsgCompleted](CreateSysMsg) must be(1)
        countSysMsgEventsOf[SysMsgCompleted](TerminateSysMsg) must be(1)

        countSysMsgEventsOf[SysMsgCompleted, SuperviseSysMsg] must be(1)
        countSysMsgEventsOf[SysMsgCompleted, RecreateSysMsg] must be(1)
        countSysMsgEventsOf[SysMsgCompleted, ChildTerminatedSysMsg] must be(1)

        countEventsOf[ActorRequested] must be(1)
        countEventsOf[ActorCreated] must be(1)
        countEventsOf[ActorTold] must be(6)
        countEventsOf[ActorAutoReceived] must be(2)
        countEventsOf[ActorAutoCompleted] must be(2)
        countEventsOf[ActorReceived] must be(5)
        countEventsOf[ActorCompleted] must be(5)
        countEventsOf[ActorAsked] must be(2)
        countEventsOf[ActorFailed] must be(1)
        countEventsOf[TempActorCreated] must be(2)
        countEventsOf[TempActorStopped] must be(2)

        countEventsOf[FutureCreated] must be(2)
        countEventsOf[FutureSucceeded] must be(2)
        countEventsOf[FutureCallbackAdded] must be(2)
        countEventsOf[FutureCallbackStarted] must be(2)
        countEventsOf[FutureCallbackCompleted] must be(2)
        countEventsOf[FutureAwaited] must be(2)

        countEventsOf[ScheduledOnce] must be(2)
        countEventsOf[ScheduledCancelled] must be(2)

        countEventsOf[EventStreamError] must be(1)
      }
    }
  }
}

class Akka21TracingSpec extends AtmosCollectSpec {

  "Tracing" must {

    "store all trace events in memory" in {
      eventCheck(expected = 79) {
        countTraces must be(1)

        countEventsOf[GroupStarted] must be(2)
        countEventsOf[GroupEnded] must be(2)

        countEventsOf[Marker] must be(6)

        countEventsOf[TopLevelActorCreated] must be(3)
        countEventsOf[TopLevelActorRequested] must be(3)

        countEventsOf[SysMsgDispatched] must be(12)
        countEventsOf[SysMsgReceived] must be(12)
        countEventsOf[SysMsgCompleted] must be(12)

        countSysMsgEventsOf[SysMsgCompleted](CreateSysMsg) must be(3)
        countSysMsgEventsOf[SysMsgCompleted](TerminateSysMsg) must be(3)

        countSysMsgEventsOf[SysMsgCompleted, SuperviseSysMsg] must be(3)
        countSysMsgEventsOf[SysMsgCompleted, ChildTerminatedSysMsg] must be(3)

        countEventsOf[ActorRequested] must be(3)
        countEventsOf[ActorCreated] must be(3)
        countEventsOf[ActorTold] must be(7)
        countEventsOf[ActorAutoReceived] must be(3)
        countEventsOf[ActorAutoCompleted] must be(3)
        countEventsOf[ActorReceived] must be(4)
        countEventsOf[ActorCompleted] must be(4)
      }
    }

    "record actor failed events" in {
      eventCheck(expected = 48) {
        countTraces must be(1)

        countEventsOf[GroupStarted] must be(1)
        countEventsOf[GroupEnded] must be(1)

        countEventsOf[TopLevelActorCreated] must be(1)
        countEventsOf[TopLevelActorRequested] must be(1)

        countEventsOf[SysMsgDispatched] must be(5)
        countEventsOf[SysMsgReceived] must be(5)
        countEventsOf[SysMsgCompleted] must be(5)

        countSysMsgEventsOf[SysMsgCompleted](CreateSysMsg) must be(1)
        countSysMsgEventsOf[SysMsgCompleted](TerminateSysMsg) must be(1)

        countSysMsgEventsOf[SysMsgCompleted, SuperviseSysMsg] must be(1)
        countSysMsgEventsOf[SysMsgCompleted, RecreateSysMsg] must be(1)
        countSysMsgEventsOf[SysMsgCompleted, ChildTerminatedSysMsg] must be(1)

        countEventsOf[ActorRequested] must be(1)
        countEventsOf[ActorCreated] must be(1)
        countEventsOf[ActorTold] must be(4)
        countEventsOf[ActorAutoReceived] must be(2)
        countEventsOf[ActorAutoCompleted] must be(2)
        countEventsOf[ActorReceived] must be(3)
        countEventsOf[ActorCompleted] must be(3)
        countEventsOf[ActorAsked] must be(1)
        countEventsOf[ActorFailed] must be(1)
        countEventsOf[TempActorCreated] must be(1)
        countEventsOf[TempActorStopped] must be(1)

        countEventsOf[FutureCreated] must be(1)
        countEventsOf[FutureSucceeded] must be(1)
        countEventsOf[FutureCallbackAdded] must be(1)
        countEventsOf[FutureCallbackStarted] must be(1)
        countEventsOf[FutureCallbackCompleted] must be(1)
        countEventsOf[FutureAwaited] must be(1)

        countEventsOf[ScheduledOnce] must be(1)
        countEventsOf[ScheduledCancelled] must be(1)

        countEventsOf[EventStreamError] must be(1)
      }
    }
  }
}

class Akka22Scala210TracingSpec extends Akka22TracingSpec
class Akka22Scala211TracingSpec extends Akka22TracingSpec

abstract class Akka22TracingSpec extends AtmosCollectSpec {

  "Tracing" must {

    "store all trace events in memory" in {
      eventCheck(expected = 79) {
        countTraces must be(1)

        countEventsOf[GroupStarted] must be(2)
        countEventsOf[GroupEnded] must be(2)

        countEventsOf[Marker] must be(6)

        countEventsOf[TopLevelActorCreated] must be(3)
        countEventsOf[TopLevelActorRequested] must be(3)

        countEventsOf[SysMsgDispatched] must be(12)
        countEventsOf[SysMsgReceived] must be(12)
        countEventsOf[SysMsgCompleted] must be(12)

        countSysMsgEventsOf[SysMsgCompleted](CreateSysMsg) must be(3)
        countSysMsgEventsOf[SysMsgCompleted](TerminateSysMsg) must be(3)

        countSysMsgEventsOf[SysMsgCompleted, SuperviseSysMsg] must be(3)
        countSysMsgEventsOf[SysMsgCompleted, DeathWatchSysMsg] must be(3)

        countEventsOf[ActorRequested] must be(3)
        countEventsOf[ActorCreated] must be(3)
        countEventsOf[ActorTold] must be(7)
        countEventsOf[ActorAutoReceived] must be(3)
        countEventsOf[ActorAutoCompleted] must be(3)
        countEventsOf[ActorReceived] must be(4)
        countEventsOf[ActorCompleted] must be(4)
      }
    }

    "record actor failed events" in {
      eventCheck(expected = 48) {
        countTraces must be(1)

        countEventsOf[GroupStarted] must be(1)
        countEventsOf[GroupEnded] must be(1)

        countEventsOf[TopLevelActorCreated] must be(1)
        countEventsOf[TopLevelActorRequested] must be(1)

        countEventsOf[SysMsgDispatched] must be(5)
        countEventsOf[SysMsgReceived] must be(6)
        countEventsOf[SysMsgCompleted] must be(6)

        countSysMsgEventsOf[SysMsgCompleted](CreateSysMsg) must be(1)
        countSysMsgEventsOf[SysMsgCompleted](TerminateSysMsg) must be(1)

        countSysMsgEventsOf[SysMsgCompleted, SuperviseSysMsg] must be(1)
        countSysMsgEventsOf[SysMsgCompleted, FailedSysMsg] must be(1)
        countSysMsgEventsOf[SysMsgCompleted, RecreateSysMsg] must be(1)
        countSysMsgEventsOf[SysMsgCompleted, DeathWatchSysMsg] must be(1)

        countEventsOf[ActorRequested] must be(1)
        countEventsOf[ActorCreated] must be(1)
        countEventsOf[ActorTold] must be(4)
        countEventsOf[ActorAutoReceived] must be(1)
        countEventsOf[ActorAutoCompleted] must be(1)
        countEventsOf[ActorReceived] must be(3)
        countEventsOf[ActorCompleted] must be(3)
        countEventsOf[ActorAsked] must be(1)
        countEventsOf[ActorFailed] must be(1)
        countEventsOf[TempActorCreated] must be(1)
        countEventsOf[TempActorStopped] must be(1)

        countEventsOf[FutureCreated] must be(1)
        countEventsOf[FutureSucceeded] must be(1)
        countEventsOf[FutureCallbackAdded] must be(1)
        countEventsOf[FutureCallbackStarted] must be(1)
        countEventsOf[FutureCallbackCompleted] must be(1)
        countEventsOf[FutureAwaited] must be(1)

        countEventsOf[ScheduledOnce] must be(1)
        countEventsOf[ScheduledCancelled] must be(1)

        countEventsOf[EventStreamError] must be(1)
      }
    }
  }
}
