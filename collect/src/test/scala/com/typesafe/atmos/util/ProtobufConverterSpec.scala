/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */
package com.typesafe.atmos.util

import com.typesafe.atmos.trace._
import com.typesafe.atmos.uuid.UUID
import org.scalatest.matchers.{ ShouldMatchers, MustMatchers }
import org.scalatest.WordSpec
import scala.collection.JavaConverters._

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class ProtobufConverterSpec extends WordSpec with MustMatchers with ShouldMatchers {
  val actorSystemName = "ProtobufConverterSpec"

  "ProtobufConverter" must {
    "convert UUID" in {
      val uuid1 = new UUID
      val uuid2 = new UUID
      val protoUuid1 = ProtobufConverter.toProtoUuid(uuid1)
      val protoUuid2 = ProtobufConverter.toProtoUuid(uuid2)
      val result1 = ProtobufConverter.fromProtoUuid(protoUuid1)
      val result2 = ProtobufConverter.fromProtoUuid(protoUuid2)
      result1 must equal(uuid1)
      result2 must equal(uuid2)
    }

    "convert to protobuf for all annotation types" in {
      val actorInfo1 = ActorInfo("actor1", Some("dispatcher"), true, false, Set("tag"))
      val actorInfo2 = ActorInfo("actor2", None, false, true, Set.empty[String])
      val actorSelectionInfo = ActorSelectionInfo(actorInfo1, "/selection/path")
      val futureInfo = FutureInfo(new UUID, "dispatcher")
      val taskInfo = TaskInfo(new UUID, "dispatcher")
      val iterateeInfo = IterateeInfo(new UUID)
      // val traceId = new UUID
      // val baseTime = System.nanoTime
      val requestInfo = ActionRequestInfo(100L,
        Map[String, String]("key1" -> "value1", "key2" -> "value2"),
        "uri",
        "path",
        "method",
        "version",
        Map[String, Seq[String]]("qkey1" -> Seq("qvalue1-1", "qvalue1-2"), "qkey2" -> Seq("qvalue2-1", "qvalue2-2")),
        Map[String, Seq[String]]("hkey1" -> Seq("hvalue1-1", "hvalue1-2"), "hkey2" -> Seq("hvalue2-1", "hvalue2-2")))

      val dispatcherMetrics = DispatcherMetrics(1, 2, 3L, "rejectedHandler", 4, 5L, 6L, 7L, 8L, 9L)

      val additionalMetrics = AdditionalSystemMetrics(
        CpuSystemMetrics(1.2, 2.3, 3.4, 4.5, 5.6, 6.7, 7.8, 9L),
        MemorySystemMetrics(1.2, 2L, 3L),
        NetworkSystemMetrics(1L, 2L, 3L, 4L, 5L, 6L))

      val systemMetrics1 = SystemMetrics(1L, 2L, 3L, 4, 5, 6, 7, 8L, 9L, 10L, 11L, 12L, 13L, 1.4, 1.5, 1.6, Some(additionalMetrics))
      val systemMetrics2 = SystemMetrics(1L, 2L, 3L, 4, 5, 6, 7, 8L, 9L, 10L, 11L, 12L, 13L, 1.4, 1.5, 1.6, None)

      val batch = Batch(Seq(TraceEvents(Seq(
        TraceEvent(GroupStarted("name"), actorSystemName),
        TraceEvent(GroupEnded("name"), actorSystemName),

        TraceEvent(MarkerStarted("name"), actorSystemName),
        TraceEvent(Marker("name", "data"), actorSystemName),
        TraceEvent(MarkerEnded("name"), actorSystemName),

        TraceEvent(SystemStarted(1L), actorSystemName),
        TraceEvent(SystemShutdown(2L), actorSystemName),

        TraceEvent(TopLevelActorRequested(actorInfo1, "name"), actorSystemName),
        TraceEvent(TopLevelActorCreated(actorInfo1), actorSystemName),

        TraceEvent(SysMsgDispatched(actorInfo1, CreateSysMsg), actorSystemName),
        TraceEvent(SysMsgDispatched(actorInfo1, SuspendSysMsg), actorSystemName),
        TraceEvent(SysMsgDispatched(actorInfo1, ResumeSysMsg), actorSystemName),
        TraceEvent(SysMsgDispatched(actorInfo1, TerminateSysMsg), actorSystemName),
        TraceEvent(SysMsgReceived(actorInfo1, RecreateSysMsg("cause")), actorSystemName),
        TraceEvent(SysMsgReceived(actorInfo1, SuperviseSysMsg(actorInfo2)), actorSystemName),
        TraceEvent(SysMsgReceived(actorInfo1, ChildTerminatedSysMsg(actorInfo2)), actorSystemName),
        TraceEvent(SysMsgReceived(actorInfo1, LinkSysMsg(actorInfo2)), actorSystemName),
        TraceEvent(SysMsgCompleted(actorInfo1, UnlinkSysMsg(actorInfo2)), actorSystemName),
        TraceEvent(SysMsgCompleted(actorInfo1, WatchSysMsg(actorInfo1, actorInfo2)), actorSystemName),
        TraceEvent(SysMsgCompleted(actorInfo1, UnwatchSysMsg(actorInfo1, actorInfo2)), actorSystemName),
        TraceEvent(SysMsgCompleted(actorInfo1, NoMessageSysMsg), actorSystemName),
        TraceEvent(SysMsgCompleted(actorInfo1, FailedSysMsg(actorInfo1, "cause")), actorSystemName),
        TraceEvent(SysMsgCompleted(actorInfo1, DeathWatchSysMsg(actorInfo1, true, false)), actorSystemName),

        TraceEvent(ActorRequested(actorInfo1, actorInfo2), actorSystemName),
        TraceEvent(ActorCreated(actorInfo1), actorSystemName),
        TraceEvent(ActorTold(actorInfo1, "message", Some(actorInfo2)), actorSystemName),
        TraceEvent(ActorTold(actorInfo1, "message", None), actorSystemName),
        TraceEvent(ActorAutoReceived(actorInfo1, "message"), actorSystemName),
        TraceEvent(ActorAutoCompleted(actorInfo1, "message"), actorSystemName),
        TraceEvent(ActorReceived(actorInfo1, "message"), actorSystemName),
        TraceEvent(ActorCompleted(actorInfo1, "message"), actorSystemName),
        TraceEvent(ActorAsked(actorInfo1, "message"), actorSystemName),
        TraceEvent(ActorFailed(actorInfo1, "message", actorInfo2), actorSystemName),
        TraceEvent(TempActorCreated(actorInfo1), actorSystemName),
        TraceEvent(TempActorStopped(actorInfo1), actorSystemName),

        TraceEvent(ActorSelectionTold(actorSelectionInfo, "message", Some(actorInfo2)), actorSystemName),
        TraceEvent(ActorSelectionAsked(actorSelectionInfo, "message"), actorSystemName),

        TraceEvent(FutureCreated(futureInfo), actorSystemName),
        TraceEvent(FutureScheduled(futureInfo, taskInfo), actorSystemName),
        TraceEvent(FutureSucceeded(futureInfo, "result", NoInfo), actorSystemName),
        TraceEvent(FutureSucceeded(futureInfo, "result", actorInfo1), actorSystemName),
        TraceEvent(FutureSucceeded(futureInfo, "result", futureInfo), actorSystemName),
        TraceEvent(FutureSucceeded(futureInfo, "result", taskInfo), actorSystemName),
        TraceEvent(FutureFailed(futureInfo, "exception"), actorSystemName),
        TraceEvent(FutureCallbackAdded(futureInfo), actorSystemName),
        TraceEvent(FutureCallbackStarted(futureInfo), actorSystemName),
        TraceEvent(FutureCallbackCompleted(futureInfo), actorSystemName),
        TraceEvent(FutureAwaited(futureInfo), actorSystemName),
        TraceEvent(FutureAwaitTimedOut(futureInfo, "duration"), actorSystemName),

        TraceEvent(RunnableScheduled(taskInfo), actorSystemName),
        TraceEvent(RunnableStarted(taskInfo), actorSystemName),
        TraceEvent(RunnableCompleted(taskInfo), actorSystemName),

        TraceEvent(ScheduledOnce(taskInfo, "delay"), actorSystemName),
        TraceEvent(ScheduledStarted(taskInfo), actorSystemName),
        TraceEvent(ScheduledCompleted(taskInfo), actorSystemName),
        TraceEvent(ScheduledCancelled(taskInfo), actorSystemName),

        TraceEvent(RemoteMessageSent(actorInfo1, "message", 7), actorSystemName),
        TraceEvent(RemoteMessageReceived(actorInfo1, "message", 7), actorSystemName),
        TraceEvent(RemoteMessageCompleted(actorInfo1, "message"), actorSystemName),

        TraceEvent(RemoteStatus(RemoteStatusType.RemoteClientErrorType, Some("serverNode"), Some("clientNode"), Some("cause")), actorSystemName),
        TraceEvent(RemoteStatus(RemoteStatusType.RemoteClientErrorType, None, None, None), actorSystemName),

        TraceEvent(RemotingLifecycle(RemotingLifecycleEventType.AssociationErrorEventType, Some("local"), Some("remote"), Some(true), Some("cause")), actorSystemName),
        TraceEvent(RemotingLifecycle(RemotingLifecycleEventType.RemotingListenEventType, listenAddresses = Set("1", "2", "3")), actorSystemName),

        TraceEvent(EventStreamError("message"), actorSystemName),
        TraceEvent(EventStreamWarning("message"), actorSystemName),
        TraceEvent(EventStreamDeadLetter("message", actorInfo1, actorInfo2), actorSystemName),
        TraceEvent(EventStreamUnhandledMessage("message", actorInfo1, actorInfo2), actorSystemName),

        TraceEvent(DispatcherStatus("dispatcher", "dispatcherType", dispatcherMetrics), actorSystemName),
        TraceEvent(systemMetrics1, actorSystemName),
        TraceEvent(systemMetrics2, actorSystemName),

        TraceEvent(DeadlockedThreads("message", Set("a", "b")), actorSystemName),
        TraceEvent(DeadlockedThreads("message", Set.empty[String]), actorSystemName),

        TraceEvent(ActionInvoked(ActionInvocationInfo("controller",
          "method",
          "pattern",
          100L,
          "uri",
          "path",
          "GET",
          "1.1",
          "remoteAddress",
          Some("host"),
          Some("domain"),
          Some(Map("key1" -> "value1", "key2" -> "value2")))), actorSystemName),
        TraceEvent(ActionInvoked(ActionInvocationInfo("controller",
          "method",
          "pattern",
          100L,
          "uri",
          "path",
          "GET",
          "1.1",
          "remoteAddress",
          None,
          None,
          None)), actorSystemName),
        TraceEvent(ActionResolved(ActionResolvedInfo("controller",
          "method",
          List("int", "string"),
          "GET",
          "comments",
          "path")), actorSystemName),
        TraceEvent(ActionRouteRequest(
          requestInfo,
          ActionRouteRequestResult.EssentialAction),
          actorSystemName),
        TraceEvent(ActionError(
          requestInfo,
          "message",
          Seq("stack", "trace")),
          actorSystemName),
        TraceEvent(ActionHandlerNotFound(
          requestInfo),
          actorSystemName),
        TraceEvent(ActionBadRequest(
          requestInfo,
          "error"),
          actorSystemName),
        TraceEvent(ActionChunkedResult(ActionResultInfo(200)), actorSystemName),
        TraceEvent(ActionSimpleResult(ActionResultInfo(200)), actorSystemName),
        TraceEvent(ActionAsyncResult, actorSystemName),
        TraceEvent(ActionResultGenerationStart, actorSystemName),
        TraceEvent(ActionResultGenerationEnd, actorSystemName),
        TraceEvent(ActionChunkedInputStart, actorSystemName),
        TraceEvent(ActionChunkedInputEnd, actorSystemName),
        TraceEvent(IterateeCreated(iterateeInfo), actorSystemName),
        TraceEvent(IterateeFolded(iterateeInfo), actorSystemName),
        TraceEvent(IterateeDone(iterateeInfo), actorSystemName),
        TraceEvent(IterateeError(iterateeInfo), actorSystemName),
        TraceEvent(IterateeContinued(iterateeInfo, IterateeInput.El("test"), iterateeInfo), actorSystemName),
        TraceEvent(IterateeContinued(iterateeInfo, IterateeInput.Empty, iterateeInfo), actorSystemName),
        TraceEvent(IterateeContinued(iterateeInfo, IterateeInput.EOF, iterateeInfo), actorSystemName),
        TraceEvent(NettyHttpReceivedStart, actorSystemName),
        TraceEvent(NettyHttpReceivedEnd, actorSystemName),
        TraceEvent(NettyPlayReceivedStart, actorSystemName),
        TraceEvent(NettyPlayReceivedEnd, actorSystemName),
        TraceEvent(NettyResponseHeader(100), actorSystemName),
        TraceEvent(NettyResponseBody(100), actorSystemName),
        TraceEvent(NettyWriteChunk(10, 100), actorSystemName),
        TraceEvent(NettyReadBytes(100), actorSystemName)))))

      val protoBatch = ProtobufConverter.toProto(batch)

      // check round-trip
      val deserialized = ProtobufConverter.fromProto(protoBatch)
      // check event-by-event for better indication of what went wrong
      val eventPairs = deserialized.payload.flatMap(_.events).zip(batch.payload.flatMap(_.events))
      for (pair ← eventPairs)
        pair._1 must equal(pair._2)
      // check the whole thing for paranoia
      deserialized must equal(batch)
    }

    "convert empty batch" in {
      val batch = Batch(Seq[TraceEvents]())
      val result = ProtobufConverter.fromProto(ProtobufConverter.toProto(batch))
      result.payload.size must equal(0)
    }

    "convert batch containing 1 empty trace events" in {
      val batch = Batch(Seq[TraceEvents](TraceEvents(Seq[TraceEvent]())))
      val result = ProtobufConverter.fromProto(ProtobufConverter.toProto(batch))
      result.payload.size must equal(1)
      result.payload.head.events.size must equal(0)
    }

    "convert batch containing 1 non-empty trace events" in {
      val actorCreated = ActorCreated(ActorInfo("a1", Some("d1"), true, false, Set[String]()))
      val te1UuidId = new UUID
      val te1UuidTrace = new UUID
      val te1UuidLocal = new UUID
      val te1UuidParent = new UUID
      val te1Now = System.currentTimeMillis
      val traceEventCreated = TraceEvent(actorCreated, te1UuidId, te1UuidTrace, te1UuidLocal, te1UuidParent, 1, "n1", "h1", actorSystemName, te1Now, te1Now)

      val actorAsked = ActorAsked(ActorInfo("a2", None, false, true, Set[String]("t1", "t2")), "msg1")
      val te2UuidId = new UUID
      val te2UuidTrace = new UUID
      val te2UuidLocal = new UUID
      val te2UuidParent = new UUID
      val te2Now = System.currentTimeMillis + 1
      val traceEventAsked = TraceEvent(actorAsked, te2UuidId, te2UuidTrace, te2UuidLocal, te2UuidParent, 2, "n2", "h2", actorSystemName, te2Now, te2Now)

      val batch = Batch(Seq[TraceEvents](TraceEvents(Seq[TraceEvent](traceEventCreated, traceEventAsked))))
      val result = ProtobufConverter.fromProto(ProtobufConverter.toProto(batch))

      result.payload.size must equal(1)
      result.payload.head.events.size must equal(2)

      val created = result.payload.head.events.head
      created.id must equal(te1UuidId)
      created.trace must equal(te1UuidTrace)
      created.local must equal(te1UuidLocal)
      created.parent must equal(te1UuidParent)
      created.node must equal("n1")
      created.host must equal("h1")
      created.timestamp must equal(te1Now)
      created.nanoTime must equal(te1Now)
      created.annotation.getClass must equal(classOf[ActorCreated])
      val resultAC = created.annotation.asInstanceOf[ActorCreated]
      resultAC.info.path must equal("a1")
      resultAC.info.dispatcher.get must equal("d1")
      resultAC.info.remote must be(true)
      resultAC.info.router must be(false)
      resultAC.info.tags.size must equal(0)

      val asked = result.payload.head.events.tail.head
      asked.id must equal(te2UuidId)
      asked.trace must equal(te2UuidTrace)
      asked.local must equal(te2UuidLocal)
      asked.parent must equal(te2UuidParent)
      asked.node must equal("n2")
      asked.host must equal("h2")
      asked.timestamp must equal(te2Now)
      asked.nanoTime must equal(te2Now)
      asked.annotation.getClass must equal(classOf[ActorAsked])
      val resultAA = asked.annotation.asInstanceOf[ActorAsked]
      resultAA.info.path must equal("a2")
      resultAA.info.dispatcher must be(None)
      resultAA.info.remote must be(false)
      resultAA.info.router must be(true)
      resultAA.info.tags.size must equal(2)
      resultAA.info.tags.contains("t1") must be(true)
      resultAA.info.tags.contains("t2") must be(true)
    }

    "convert batch containing 2 trace events" in {
      val batch = Batch(Seq[TraceEvents](TraceEvents(Seq[TraceEvent]()), TraceEvents(Seq[TraceEvent]())))
      val result = ProtobufConverter.fromProto(ProtobufConverter.toProto(batch))
      result.payload.size must equal(2)
      result.payload.head.events.size must equal(0)
      result.payload.tail.head.events.size must equal(0)
    }
  }
}
