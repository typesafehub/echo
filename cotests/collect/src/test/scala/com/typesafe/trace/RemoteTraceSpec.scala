/**
 *  Copyright (C) 2009-2012 Typesafe Inc. <http://www.typesafe.com>
 */

package com.typesafe.trace

import com.typesafe.trace.test.EchoCollectSpec
import scala.concurrent.duration._

object RemoteTraceSpec {
  val config20 = """
    activator.trace.receive.max-connections = 2
  """

  val config21 = """
    activator.trace.receive.max-connections = 4
  """

  val config22 = """
    activator.trace.receive.max-connections = 4
  """

  val config23 = """
    activator.trace.receive.max-connections = 4
  """
}

class Akka20RemoteTraceSpec extends EchoCollectSpec(RemoteTraceSpec.config20) {

  override def cotestNodes = 3
  override def includeSystemStartedEvents = true

  "Remote tracing" must {

    "trace across nodes" in {
      barrier("setup")

      barrier("start")

      barrier("check-trace")

      eventCheck("1", expected = 157) {
        countEventsOf[SystemStarted] should be(2)
        countEventsOf[GroupStarted] should be(1)
        countEventsOf[GroupEnded] should be(1)

        countEventsOf[TopLevelActorRequested] should be(2)
        countEventsOf[TopLevelActorCreated] should be(2)

        countEventsOf[SysMsgDispatched] should be(8)
        countEventsOf[SysMsgReceived] should be(8)
        countEventsOf[SysMsgCompleted] should be(8)

        countEventsOf[ActorRequested] should be(3)
        countEventsOf[ActorCreated] should be(3)
        countEventsOf[ActorAsked] should be(5)
        countEventsOf[ActorTold] should be(20)
        countEventsOf[ActorAutoReceived] should be(1)
        countEventsOf[ActorAutoCompleted] should be(1)
        countEventsOf[ActorReceived] should be(15)
        countEventsOf[ActorCompleted] should be(15)

        countEventsOf[FutureCreated] should be(5)
        countEventsOf[FutureCallbackAdded] should be(5)
        countEventsOf[FutureCallbackStarted] should be(5)
        countEventsOf[FutureCallbackCompleted] should be(5)
        countEventsOf[FutureSucceeded] should be(5)
        countEventsOf[FutureAwaited] should be(5)

        countEventsOf[TempActorCreated] should be(5)
        countEventsOf[TempActorStopped] should be(5)

        countEventsOf[ScheduledOnce] should be(5)
        countEventsOf[ScheduledCancelled] should be(5)

        countEventsOf[RemoteMessageSent] should be(4)
        countEventsOf[RemoteMessageReceived] should be(4)
        countEventsOf[RemoteMessageCompleted] should be(4)

        annotationsOf[RemoteMessageSent].head.messageSize should be > 0
      }

      barrier("stop")

      eventCheck("2", expected = 12) {
        countEventsOf[GroupStarted] should be(1)
        countEventsOf[GroupEnded] should be(1)

        countEventsOf[RemoteMessageSent] should be(1)
        countEventsOf[RemoteMessageReceived] should be(1)
        countEventsOf[RemoteMessageCompleted] should be(1)

        countSysMsgEventsOf[SysMsgDispatched](TerminateSysMsg) should be(2)
        countSysMsgEventsOf[SysMsgReceived](TerminateSysMsg) should be(1)
        countSysMsgEventsOf[SysMsgCompleted](TerminateSysMsg) should be(1)

        countSysMsgEventsOf[SysMsgDispatched, ChildTerminatedSysMsg] should be(1)
        countSysMsgEventsOf[SysMsgReceived, ChildTerminatedSysMsg] should be(1)
        countSysMsgEventsOf[SysMsgCompleted, ChildTerminatedSysMsg] should be(1)
      }
    }
  }
}

class Akka21RemoteTraceSpec extends EchoCollectSpec(RemoteTraceSpec.config21) {

  override def cotestNodes = 3
  override def includeSystemStartedEvents = true

  "Remote tracing" must {

    "trace across nodes" in {
      barrier("setup")

      barrier("start")

      barrier("check-trace")

      eventCheck("1", expected = 132) {
        countEventsOf[SystemStarted] should be(2)
        countEventsOf[GroupStarted] should be(1)
        countEventsOf[GroupEnded] should be(1)

        countEventsOf[TopLevelActorRequested] should be(2)
        countEventsOf[TopLevelActorCreated] should be(2)

        countEventsOf[SysMsgDispatched] should be(8)
        countEventsOf[SysMsgReceived] should be(8)
        countEventsOf[SysMsgCompleted] should be(8)

        countEventsOf[ActorRequested] should be(3)
        countEventsOf[ActorCreated] should be(3)
        countEventsOf[ActorAsked] should be(3)
        countEventsOf[ActorTold] should be(16)
        countEventsOf[ActorAutoReceived] should be(1)
        countEventsOf[ActorAutoCompleted] should be(1)
        countEventsOf[ActorReceived] should be(11)
        countEventsOf[ActorCompleted] should be(11)

        countEventsOf[FutureCreated] should be(3)
        countEventsOf[FutureCallbackAdded] should be(6)
        countEventsOf[FutureCallbackStarted] should be(6)
        countEventsOf[FutureCallbackCompleted] should be(6)
        countEventsOf[FutureSucceeded] should be(3)
        countEventsOf[FutureAwaited] should be(3)

        countEventsOf[TempActorCreated] should be(3)
        countEventsOf[TempActorStopped] should be(3)

        countEventsOf[ScheduledOnce] should be(3)
        countEventsOf[ScheduledCancelled] should be(3)

        countEventsOf[RemoteMessageSent] should be(4)
        countEventsOf[RemoteMessageReceived] should be(4)
        countEventsOf[RemoteMessageCompleted] should be(4)

        annotationsOf[RemoteMessageSent].head.messageSize should be > 0
      }

      barrier("stop")

      eventCheck("2", expected = 12) {
        countEventsOf[GroupStarted] should be(1)
        countEventsOf[GroupEnded] should be(1)

        countEventsOf[RemoteMessageSent] should be(1)
        countEventsOf[RemoteMessageReceived] should be(1)
        countEventsOf[RemoteMessageCompleted] should be(1)

        countSysMsgEventsOf[SysMsgDispatched](TerminateSysMsg) should be(2)
        countSysMsgEventsOf[SysMsgReceived](TerminateSysMsg) should be(1)
        countSysMsgEventsOf[SysMsgCompleted](TerminateSysMsg) should be(1)

        countSysMsgEventsOf[SysMsgDispatched, ChildTerminatedSysMsg] should be(1)
        countSysMsgEventsOf[SysMsgReceived, ChildTerminatedSysMsg] should be(1)
        countSysMsgEventsOf[SysMsgCompleted, ChildTerminatedSysMsg] should be(1)
      }
    }
  }
}

class Akka22RemoteTraceSpec extends EchoCollectSpec(RemoteTraceSpec.config22) {
  override def cotestNodes = 3
  override def includeSystemStartedEvents = true
  val traceAcrossNodesEventCount = 57
  val traceAcrossNodesTestEventCount = 110
  val traceAcrossNodesTracesCount = 7
  val traceAcrossNodesFutureCallbackAddedCount = 2
  val traceAcrossNodesFutureCallbackStartedCount = 2
  val traceAcrossNodesFutureCallbackCompletedCount = 2
  val traceAcrossNodesTestFutureCallbackAddedCount = 6
  val traceAcrossNodesTestFutureCallbackStartedCount = 6
  val traceAcrossNodesTestFutureCallbackCompletedCount = 6

  "Remote tracing" must {

    "trace across nodes" in {
      barrier("setup")

      eventCheck("create-and-identify", expected = traceAcrossNodesEventCount) {
        countTraces should be(traceAcrossNodesTracesCount)

        countEventsOf[SystemStarted] should be(2)

        countEventsOf[TopLevelActorRequested] should be(2)
        countEventsOf[TopLevelActorCreated] should be(2)

        countEventsOf[SysMsgDispatched] should be(4)
        countEventsOf[SysMsgReceived] should be(4)
        countEventsOf[SysMsgCompleted] should be(4)

        countEventsOf[ActorRequested] should be(2)
        countEventsOf[ActorCreated] should be(2)
        countEventsOf[ActorTold] should be(6)
        countEventsOf[ActorAutoReceived] should be(3)
        countEventsOf[ActorAutoCompleted] should be(3)
        countEventsOf[ActorReceived] should be(1)
        countEventsOf[ActorCompleted] should be(1)

        countEventsOf[ActorSelectionAsked] should be(1)
        countEventsOf[ActorSelectionTold] should be(1)

        countEventsOf[FutureCreated] should be(1)
        countEventsOf[FutureCallbackAdded] should be(traceAcrossNodesFutureCallbackAddedCount)
        countEventsOf[FutureCallbackStarted] should be(traceAcrossNodesFutureCallbackStartedCount)
        countEventsOf[FutureCallbackCompleted] should be(traceAcrossNodesFutureCallbackCompletedCount)
        countEventsOf[FutureSucceeded] should be(1)
        countEventsOf[FutureAwaited] should be(1)

        countEventsOf[TempActorCreated] should be(1)
        countEventsOf[TempActorStopped] should be(1)

        countEventsOf[ScheduledOnce] should be(1)
        countEventsOf[ScheduledCancelled] should be(1)

        countEventsOf[RemoteMessageSent] should be(2)
        countEventsOf[RemoteMessageReceived] should be(2)
        countEventsOf[RemoteMessageCompleted] should be(2)
      }

      barrier("start")

      barrier("check-trace")

      eventCheck("test", expected = traceAcrossNodesTestEventCount) {
        countEventsOf[GroupStarted] should be(1)
        countEventsOf[GroupEnded] should be(1)

        countEventsOf[SysMsgDispatched] should be(4)
        countEventsOf[SysMsgReceived] should be(4)
        countEventsOf[SysMsgCompleted] should be(4)

        countEventsOf[ActorRequested] should be(1)
        countEventsOf[ActorCreated] should be(1)
        countEventsOf[ActorAsked] should be(3)
        countEventsOf[ActorTold] should be(16)
        countEventsOf[ActorAutoReceived] should be(1)
        countEventsOf[ActorAutoCompleted] should be(1)
        countEventsOf[ActorReceived] should be(11)
        countEventsOf[ActorCompleted] should be(11)

        countEventsOf[FutureCreated] should be(3)
        countEventsOf[FutureCallbackAdded] should be(traceAcrossNodesTestFutureCallbackAddedCount)
        countEventsOf[FutureCallbackStarted] should be(traceAcrossNodesTestFutureCallbackStartedCount)
        countEventsOf[FutureCallbackCompleted] should be(traceAcrossNodesTestFutureCallbackCompletedCount)
        countEventsOf[FutureSucceeded] should be(3)
        countEventsOf[FutureAwaited] should be(3)

        countEventsOf[TempActorCreated] should be(3)
        countEventsOf[TempActorStopped] should be(3)

        countEventsOf[ScheduledOnce] should be(3)
        countEventsOf[ScheduledCancelled] should be(3)

        countEventsOf[RemoteMessageSent] should be(4)
        countEventsOf[RemoteMessageReceived] should be(4)
        countEventsOf[RemoteMessageCompleted] should be(4)

        annotationsOf[RemoteMessageSent].head.messageSize should be > 0
      }

      barrier("stop")

      eventCheck("stopped", expected = 12) {
        countEventsOf[GroupStarted] should be(1)
        countEventsOf[GroupEnded] should be(1)

        countEventsOf[RemoteMessageSent] should be(1)
        countEventsOf[RemoteMessageReceived] should be(1)
        countEventsOf[RemoteMessageCompleted] should be(1)

        countSysMsgEventsOf[SysMsgDispatched](TerminateSysMsg) should be(2)
        countSysMsgEventsOf[SysMsgReceived](TerminateSysMsg) should be(1)
        countSysMsgEventsOf[SysMsgCompleted](TerminateSysMsg) should be(1)

        countSysMsgEventsOf[SysMsgDispatched, DeathWatchSysMsg] should be(1)
        countSysMsgEventsOf[SysMsgReceived, DeathWatchSysMsg] should be(1)
        countSysMsgEventsOf[SysMsgCompleted, DeathWatchSysMsg] should be(1)
      }
    }
  }
}

class Akka23Scala210RemoteTraceSpec extends Akka23RemoteTraceSpec {
  val traceAcrossNodesEventCount = 57
  val traceAcrossNodesTestEventCount = 110
  val traceAcrossNodesTracesCount = 7
  val traceAcrossNodesFutureCallbackAddedCount = 2
  val traceAcrossNodesFutureCallbackStartedCount = 2
  val traceAcrossNodesFutureCallbackCompletedCount = 2
  val traceAcrossNodesTestFutureCallbackAddedCount = 6
  val traceAcrossNodesTestFutureCallbackStartedCount = 6
  val traceAcrossNodesTestFutureCallbackCompletedCount = 6
}

class Akka23Scala211RemoteTraceSpec extends Akka23RemoteTraceSpec {
  val traceAcrossNodesEventCount = 54
  val traceAcrossNodesTestEventCount = 101
  val traceAcrossNodesTracesCount = 6
  val traceAcrossNodesFutureCallbackAddedCount = 1
  val traceAcrossNodesFutureCallbackStartedCount = 1
  val traceAcrossNodesFutureCallbackCompletedCount = 1
  val traceAcrossNodesTestFutureCallbackAddedCount = 3
  val traceAcrossNodesTestFutureCallbackStartedCount = 3
  val traceAcrossNodesTestFutureCallbackCompletedCount = 3
}

abstract class Akka23RemoteTraceSpec extends EchoCollectSpec(RemoteTraceSpec.config23) {

  override def cotestNodes = 3
  override def includeSystemStartedEvents = true
  def traceAcrossNodesEventCount: Int
  def traceAcrossNodesTestEventCount: Int
  def traceAcrossNodesTracesCount: Int
  def traceAcrossNodesFutureCallbackAddedCount: Int
  def traceAcrossNodesFutureCallbackStartedCount: Int
  def traceAcrossNodesFutureCallbackCompletedCount: Int
  def traceAcrossNodesTestFutureCallbackAddedCount: Int
  def traceAcrossNodesTestFutureCallbackStartedCount: Int
  def traceAcrossNodesTestFutureCallbackCompletedCount: Int

  "Remote tracing" must {

    "trace across nodes" in {
      barrier("setup")

      eventCheck("create-and-identify", expected = traceAcrossNodesEventCount) {
        countTraces should be(traceAcrossNodesTracesCount)

        countEventsOf[SystemStarted] should be(2)

        countEventsOf[TopLevelActorRequested] should be(2)
        countEventsOf[TopLevelActorCreated] should be(2)

        countEventsOf[SysMsgDispatched] should be(4)
        countEventsOf[SysMsgReceived] should be(4)
        countEventsOf[SysMsgCompleted] should be(4)

        countEventsOf[ActorRequested] should be(2)
        countEventsOf[ActorCreated] should be(2)
        countEventsOf[ActorTold] should be(6)
        countEventsOf[ActorAutoReceived] should be(3)
        countEventsOf[ActorAutoCompleted] should be(3)
        countEventsOf[ActorReceived] should be(1)
        countEventsOf[ActorCompleted] should be(1)

        countEventsOf[ActorSelectionAsked] should be(1)
        countEventsOf[ActorSelectionTold] should be(1)

        countEventsOf[FutureCreated] should be(1)
        countEventsOf[FutureCallbackAdded] should be(traceAcrossNodesFutureCallbackAddedCount)
        countEventsOf[FutureCallbackStarted] should be(traceAcrossNodesFutureCallbackStartedCount)
        countEventsOf[FutureCallbackCompleted] should be(traceAcrossNodesFutureCallbackCompletedCount)
        countEventsOf[FutureSucceeded] should be(1)
        countEventsOf[FutureAwaited] should be(1)

        countEventsOf[TempActorCreated] should be(1)
        countEventsOf[TempActorStopped] should be(1)

        countEventsOf[ScheduledOnce] should be(1)
        countEventsOf[ScheduledCancelled] should be(1)

        countEventsOf[RemoteMessageSent] should be(2)
        countEventsOf[RemoteMessageReceived] should be(2)
        countEventsOf[RemoteMessageCompleted] should be(2)
      }

      barrier("start")

      barrier("check-trace")

      eventCheck("test", expected = traceAcrossNodesTestEventCount) {
        countEventsOf[GroupStarted] should be(1)
        countEventsOf[GroupEnded] should be(1)

        countEventsOf[SysMsgDispatched] should be(4)
        countEventsOf[SysMsgReceived] should be(4)
        countEventsOf[SysMsgCompleted] should be(4)

        countEventsOf[ActorRequested] should be(1)
        countEventsOf[ActorCreated] should be(1)
        countEventsOf[ActorAsked] should be(3)
        countEventsOf[ActorTold] should be(16)
        countEventsOf[ActorAutoReceived] should be(1)
        countEventsOf[ActorAutoCompleted] should be(1)
        countEventsOf[ActorReceived] should be(11)
        countEventsOf[ActorCompleted] should be(11)

        countEventsOf[FutureCreated] should be(3)
        countEventsOf[FutureCallbackAdded] should be(traceAcrossNodesTestFutureCallbackAddedCount)
        countEventsOf[FutureCallbackStarted] should be(traceAcrossNodesTestFutureCallbackStartedCount)
        countEventsOf[FutureCallbackCompleted] should be(traceAcrossNodesTestFutureCallbackCompletedCount)
        countEventsOf[FutureSucceeded] should be(3)
        countEventsOf[FutureAwaited] should be(3)

        countEventsOf[TempActorCreated] should be(3)
        countEventsOf[TempActorStopped] should be(3)

        countEventsOf[ScheduledOnce] should be(3)
        countEventsOf[ScheduledCancelled] should be(3)

        countEventsOf[RemoteMessageSent] should be(4)
        countEventsOf[RemoteMessageReceived] should be(4)
        countEventsOf[RemoteMessageCompleted] should be(4)

        annotationsOf[RemoteMessageSent].head.messageSize should be > 0
      }

      barrier("stop")

      eventCheck("stopped", expected = 12) {
        countEventsOf[GroupStarted] should be(1)
        countEventsOf[GroupEnded] should be(1)

        countEventsOf[RemoteMessageSent] should be(1)
        countEventsOf[RemoteMessageReceived] should be(1)
        countEventsOf[RemoteMessageCompleted] should be(1)

        countSysMsgEventsOf[SysMsgDispatched](TerminateSysMsg) should be(2)
        countSysMsgEventsOf[SysMsgReceived](TerminateSysMsg) should be(1)
        countSysMsgEventsOf[SysMsgCompleted](TerminateSysMsg) should be(1)

        countSysMsgEventsOf[SysMsgDispatched, DeathWatchSysMsg] should be(1)
        countSysMsgEventsOf[SysMsgReceived, DeathWatchSysMsg] should be(1)
        countSysMsgEventsOf[SysMsgCompleted, DeathWatchSysMsg] should be(1)
      }
    }
  }
}
