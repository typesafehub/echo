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
        countEventsOf[SystemStarted] must be(2)
        countEventsOf[GroupStarted] must be(1)
        countEventsOf[GroupEnded] must be(1)

        countEventsOf[TopLevelActorRequested] must be(2)
        countEventsOf[TopLevelActorCreated] must be(2)

        countEventsOf[SysMsgDispatched] must be(8)
        countEventsOf[SysMsgReceived] must be(8)
        countEventsOf[SysMsgCompleted] must be(8)

        countEventsOf[ActorRequested] must be(3)
        countEventsOf[ActorCreated] must be(3)
        countEventsOf[ActorAsked] must be(5)
        countEventsOf[ActorTold] must be(20)
        countEventsOf[ActorAutoReceived] must be(1)
        countEventsOf[ActorAutoCompleted] must be(1)
        countEventsOf[ActorReceived] must be(15)
        countEventsOf[ActorCompleted] must be(15)

        countEventsOf[FutureCreated] must be(5)
        countEventsOf[FutureCallbackAdded] must be(5)
        countEventsOf[FutureCallbackStarted] must be(5)
        countEventsOf[FutureCallbackCompleted] must be(5)
        countEventsOf[FutureSucceeded] must be(5)
        countEventsOf[FutureAwaited] must be(5)

        countEventsOf[TempActorCreated] must be(5)
        countEventsOf[TempActorStopped] must be(5)

        countEventsOf[ScheduledOnce] must be(5)
        countEventsOf[ScheduledCancelled] must be(5)

        countEventsOf[RemoteMessageSent] must be(4)
        countEventsOf[RemoteMessageReceived] must be(4)
        countEventsOf[RemoteMessageCompleted] must be(4)

        annotationsOf[RemoteMessageSent].head.messageSize must be > 0
      }

      barrier("stop")

      eventCheck("2", expected = 12) {
        countEventsOf[GroupStarted] must be(1)
        countEventsOf[GroupEnded] must be(1)

        countEventsOf[RemoteMessageSent] must be(1)
        countEventsOf[RemoteMessageReceived] must be(1)
        countEventsOf[RemoteMessageCompleted] must be(1)

        countSysMsgEventsOf[SysMsgDispatched](TerminateSysMsg) must be(2)
        countSysMsgEventsOf[SysMsgReceived](TerminateSysMsg) must be(1)
        countSysMsgEventsOf[SysMsgCompleted](TerminateSysMsg) must be(1)

        countSysMsgEventsOf[SysMsgDispatched, ChildTerminatedSysMsg] must be(1)
        countSysMsgEventsOf[SysMsgReceived, ChildTerminatedSysMsg] must be(1)
        countSysMsgEventsOf[SysMsgCompleted, ChildTerminatedSysMsg] must be(1)
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
        countEventsOf[SystemStarted] must be(2)
        countEventsOf[GroupStarted] must be(1)
        countEventsOf[GroupEnded] must be(1)

        countEventsOf[TopLevelActorRequested] must be(2)
        countEventsOf[TopLevelActorCreated] must be(2)

        countEventsOf[SysMsgDispatched] must be(8)
        countEventsOf[SysMsgReceived] must be(8)
        countEventsOf[SysMsgCompleted] must be(8)

        countEventsOf[ActorRequested] must be(3)
        countEventsOf[ActorCreated] must be(3)
        countEventsOf[ActorAsked] must be(3)
        countEventsOf[ActorTold] must be(16)
        countEventsOf[ActorAutoReceived] must be(1)
        countEventsOf[ActorAutoCompleted] must be(1)
        countEventsOf[ActorReceived] must be(11)
        countEventsOf[ActorCompleted] must be(11)

        countEventsOf[FutureCreated] must be(3)
        countEventsOf[FutureCallbackAdded] must be(6)
        countEventsOf[FutureCallbackStarted] must be(6)
        countEventsOf[FutureCallbackCompleted] must be(6)
        countEventsOf[FutureSucceeded] must be(3)
        countEventsOf[FutureAwaited] must be(3)

        countEventsOf[TempActorCreated] must be(3)
        countEventsOf[TempActorStopped] must be(3)

        countEventsOf[ScheduledOnce] must be(3)
        countEventsOf[ScheduledCancelled] must be(3)

        countEventsOf[RemoteMessageSent] must be(4)
        countEventsOf[RemoteMessageReceived] must be(4)
        countEventsOf[RemoteMessageCompleted] must be(4)

        annotationsOf[RemoteMessageSent].head.messageSize must be > 0
      }

      barrier("stop")

      eventCheck("2", expected = 12) {
        countEventsOf[GroupStarted] must be(1)
        countEventsOf[GroupEnded] must be(1)

        countEventsOf[RemoteMessageSent] must be(1)
        countEventsOf[RemoteMessageReceived] must be(1)
        countEventsOf[RemoteMessageCompleted] must be(1)

        countSysMsgEventsOf[SysMsgDispatched](TerminateSysMsg) must be(2)
        countSysMsgEventsOf[SysMsgReceived](TerminateSysMsg) must be(1)
        countSysMsgEventsOf[SysMsgCompleted](TerminateSysMsg) must be(1)

        countSysMsgEventsOf[SysMsgDispatched, ChildTerminatedSysMsg] must be(1)
        countSysMsgEventsOf[SysMsgReceived, ChildTerminatedSysMsg] must be(1)
        countSysMsgEventsOf[SysMsgCompleted, ChildTerminatedSysMsg] must be(1)
      }
    }
  }
}

class Akka22Scala210RemoteTraceSpec extends Akka22RemoteTraceSpec {
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

class Akka22Scala211RemoteTraceSpec extends Akka22RemoteTraceSpec {
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

abstract class Akka22RemoteTraceSpec extends EchoCollectSpec(RemoteTraceSpec.config22) {

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
        countTraces must be(traceAcrossNodesTracesCount)

        countEventsOf[SystemStarted] must be(2)

        countEventsOf[TopLevelActorRequested] must be(2)
        countEventsOf[TopLevelActorCreated] must be(2)

        countEventsOf[SysMsgDispatched] must be(4)
        countEventsOf[SysMsgReceived] must be(4)
        countEventsOf[SysMsgCompleted] must be(4)

        countEventsOf[ActorRequested] must be(2)
        countEventsOf[ActorCreated] must be(2)
        countEventsOf[ActorTold] must be(6)
        countEventsOf[ActorAutoReceived] must be(3)
        countEventsOf[ActorAutoCompleted] must be(3)
        countEventsOf[ActorReceived] must be(1)
        countEventsOf[ActorCompleted] must be(1)

        countEventsOf[ActorSelectionAsked] must be(1)
        countEventsOf[ActorSelectionTold] must be(1)

        countEventsOf[FutureCreated] must be(1)
        countEventsOf[FutureCallbackAdded] must be(traceAcrossNodesFutureCallbackAddedCount)
        countEventsOf[FutureCallbackStarted] must be(traceAcrossNodesFutureCallbackStartedCount)
        countEventsOf[FutureCallbackCompleted] must be(traceAcrossNodesFutureCallbackCompletedCount)
        countEventsOf[FutureSucceeded] must be(1)
        countEventsOf[FutureAwaited] must be(1)

        countEventsOf[TempActorCreated] must be(1)
        countEventsOf[TempActorStopped] must be(1)

        countEventsOf[ScheduledOnce] must be(1)
        countEventsOf[ScheduledCancelled] must be(1)

        countEventsOf[RemoteMessageSent] must be(2)
        countEventsOf[RemoteMessageReceived] must be(2)
        countEventsOf[RemoteMessageCompleted] must be(2)
      }

      barrier("start")

      barrier("check-trace")

      eventCheck("test", expected = traceAcrossNodesTestEventCount) {
        countEventsOf[GroupStarted] must be(1)
        countEventsOf[GroupEnded] must be(1)

        countEventsOf[SysMsgDispatched] must be(4)
        countEventsOf[SysMsgReceived] must be(4)
        countEventsOf[SysMsgCompleted] must be(4)

        countEventsOf[ActorRequested] must be(1)
        countEventsOf[ActorCreated] must be(1)
        countEventsOf[ActorAsked] must be(3)
        countEventsOf[ActorTold] must be(16)
        countEventsOf[ActorAutoReceived] must be(1)
        countEventsOf[ActorAutoCompleted] must be(1)
        countEventsOf[ActorReceived] must be(11)
        countEventsOf[ActorCompleted] must be(11)

        countEventsOf[FutureCreated] must be(3)
        countEventsOf[FutureCallbackAdded] must be(traceAcrossNodesTestFutureCallbackAddedCount)
        countEventsOf[FutureCallbackStarted] must be(traceAcrossNodesTestFutureCallbackStartedCount)
        countEventsOf[FutureCallbackCompleted] must be(traceAcrossNodesTestFutureCallbackCompletedCount)
        countEventsOf[FutureSucceeded] must be(3)
        countEventsOf[FutureAwaited] must be(3)

        countEventsOf[TempActorCreated] must be(3)
        countEventsOf[TempActorStopped] must be(3)

        countEventsOf[ScheduledOnce] must be(3)
        countEventsOf[ScheduledCancelled] must be(3)

        countEventsOf[RemoteMessageSent] must be(4)
        countEventsOf[RemoteMessageReceived] must be(4)
        countEventsOf[RemoteMessageCompleted] must be(4)

        annotationsOf[RemoteMessageSent].head.messageSize must be > 0
      }

      barrier("stop")

      eventCheck("stopped", expected = 12) {
        countEventsOf[GroupStarted] must be(1)
        countEventsOf[GroupEnded] must be(1)

        countEventsOf[RemoteMessageSent] must be(1)
        countEventsOf[RemoteMessageReceived] must be(1)
        countEventsOf[RemoteMessageCompleted] must be(1)

        countSysMsgEventsOf[SysMsgDispatched](TerminateSysMsg) must be(2)
        countSysMsgEventsOf[SysMsgReceived](TerminateSysMsg) must be(1)
        countSysMsgEventsOf[SysMsgCompleted](TerminateSysMsg) must be(1)

        countSysMsgEventsOf[SysMsgDispatched, DeathWatchSysMsg] must be(1)
        countSysMsgEventsOf[SysMsgReceived, DeathWatchSysMsg] must be(1)
        countSysMsgEventsOf[SysMsgCompleted, DeathWatchSysMsg] must be(1)
      }
    }
  }
}
