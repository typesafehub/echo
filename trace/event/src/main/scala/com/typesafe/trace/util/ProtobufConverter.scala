/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */
package com.typesafe.trace.util

import com.typesafe.trace._
import com.typesafe.trace.TraceProtocol._
import com.typesafe.trace.uuid.UUID
import scala.collection.JavaConverters._
import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer

object ProtobufConverter {

  // =========================================================================
  // Converting to protobuf
  // =========================================================================

  def toProto(batch: Batch): ProtoBatch = {
    val batchBuilder = ProtoBatch.newBuilder
    for (traceEvents ← batch.payload) {
      val traceEventsBuilder = ProtoTraceEvents.newBuilder
      for (event ← traceEvents.events) {
        val traceEvent = toProtoTraceEvent(event)
        traceEventsBuilder.addEvent(traceEvent)
      }
      batchBuilder.addTraceEvents(traceEventsBuilder.build())
    }
    batchBuilder.build()
  }

  private[util] def toProtoTraceEvent(event: TraceEvent): ProtoTraceEvent = {
    val builder = ProtoTraceEvent.newBuilder
    builder.setAnnotation(toProtoAnnotation(event))
    builder.setHost(event.host)
    builder.setId(toProtoUuid(event.id))
    builder.setLocal(toProtoUuid(event.local))
    builder.setNanoTime(event.nanoTime)
    builder.setNode(event.node)
    builder.setActorSystem(event.actorSystem)
    builder.setParent(toProtoUuid(event.parent))
    builder.setSampled(event.sampled)
    builder.setTrace(toProtoUuid(event.trace))
    builder.setTimestamp(event.timestamp)
    builder.build()
  }

  private[util] def toProtoAnnotation(event: TraceEvent): ProtoAnnotation = {
    val builder = ProtoAnnotation.newBuilder
    val dataBuilder = ProtoAnnotationData.newBuilder

    event.annotation match {

      case a: GroupStarted ⇒
        // GroupStarted(name: String)
        builder.setType(ProtoAnnotationType.GROUP_STARTED)
        dataBuilder.setString1(a.name)

      case a: GroupEnded ⇒
        // GroupEnded(name: String)
        builder.setType(ProtoAnnotationType.GROUP_ENDED)
        dataBuilder.setString1(a.name)

      case a: MarkerStarted ⇒
        // MarkerStarted(name: String)
        builder.setType(ProtoAnnotationType.MARKER_STARTED)
        dataBuilder.setString1(a.name)

      case a: Marker ⇒
        // Marker(name: String, data: String)
        builder.setType(ProtoAnnotationType.MARKER)
        dataBuilder.setString1(a.name)
        dataBuilder.setString2(a.data)

      case a: MarkerEnded ⇒
        // MarkerEnded(name: String)
        builder.setType(ProtoAnnotationType.MARKER_ENDED)
        dataBuilder.setString1(a.name)

      case a: SystemStarted ⇒
        // SystemStarted(startTime: Long)
        builder.setType(ProtoAnnotationType.SYSTEM_STARTED)
        dataBuilder.setNumber(a.startTime)

      case a: SystemShutdown ⇒
        // SystemShutdown(shutdownTime: Long)
        builder.setType(ProtoAnnotationType.SYSTEM_SHUTDOWN)
        dataBuilder.setNumber(a.shutdownTime)

      case a: TopLevelActorRequested ⇒
        // TopLevelActorRequested(guardian: ActorInfo, name: String)
        builder.setType(ProtoAnnotationType.TOP_LEVEL_ACTOR_REQUESTED)
        dataBuilder.setInfo1(toProtoActorInfo(a.guardian))
        dataBuilder.setString1(a.name)

      case a: TopLevelActorCreated ⇒
        // TopLevelActorCreated(info: ActorInfo)
        builder.setType(ProtoAnnotationType.TOP_LEVEL_ACTOR_CREATED)
        dataBuilder.setInfo1(toProtoActorInfo(a.info))

      case a: SysMsgDispatched ⇒
        // SysMsgDispatched(info: ActorInfo, message: SysMsg)
        builder.setType(ProtoAnnotationType.SYSTEM_MESSAGE_DISPATCHED)
        dataBuilder.setInfo1(toProtoActorInfo(a.info))
        toProtoSystemMessage(a.message, dataBuilder)

      case a: SysMsgReceived ⇒
        // SysMsgReceived(info: ActorInfo, message: SysMsg)
        builder.setType(ProtoAnnotationType.SYSTEM_MESSAGE_RECEIVED)
        dataBuilder.setInfo1(toProtoActorInfo(a.info))
        toProtoSystemMessage(a.message, dataBuilder)

      case a: SysMsgCompleted ⇒
        // SysMsgCompleted(info: ActorInfo, message: SysMsg)
        builder.setType(ProtoAnnotationType.SYSTEM_MESSAGE_COMPLETED)
        dataBuilder.setInfo1(toProtoActorInfo(a.info))
        toProtoSystemMessage(a.message, dataBuilder)

      case a: ActorRequested ⇒
        // ActorRequested(info: ActorInfo, actor: ActorInfo)
        builder.setType(ProtoAnnotationType.ACTOR_REQUESTED)
        dataBuilder.setInfo1(toProtoActorInfo(a.info))
        dataBuilder.setInfo2(toProtoActorInfo(a.actor))

      case a: ActorCreated ⇒
        // ActorCreated(info: ActorInfo)
        builder.setType(ProtoAnnotationType.ACTOR_CREATED)
        dataBuilder.setInfo1(toProtoActorInfo(a.info))

      case a: ActorTold ⇒
        // ActorTold(info: ActorInfo, message: String, sender: Option[ActorInfo])
        builder.setType(ProtoAnnotationType.ACTOR_TOLD)
        dataBuilder.setInfo1(toProtoActorInfo(a.info))
        dataBuilder.setString1(a.message)
        for (sender ← a.sender) dataBuilder.setInfo2(toProtoActorInfo(sender))

      case a: ActorAutoReceived ⇒
        // ActorAutoReceived(info: ActorInfo, message: String)
        builder.setType(ProtoAnnotationType.ACTOR_AUTO_RECEIVED)
        dataBuilder.setInfo1(toProtoActorInfo(a.info))
        dataBuilder.setString1(a.message)

      case a: ActorAutoCompleted ⇒
        // ActorAutoCompleted(info: ActorInfo, message: String)
        builder.setType(ProtoAnnotationType.ACTOR_AUTO_COMPLETED)
        dataBuilder.setInfo1(toProtoActorInfo(a.info))
        dataBuilder.setString1(a.message)

      case a: ActorReceived ⇒
        // ActorReceived(info: ActorInfo, message: String)
        builder.setType(ProtoAnnotationType.ACTOR_RECEIVED)
        dataBuilder.setInfo1(toProtoActorInfo(a.info))
        dataBuilder.setString1(a.message)

      case a: ActorCompleted ⇒
        // ActorCompleted(info: ActorInfo, message: String)
        builder.setType(ProtoAnnotationType.ACTOR_COMPLETED)
        dataBuilder.setInfo1(toProtoActorInfo(a.info))
        dataBuilder.setString1(a.message)

      case a: ActorAsked ⇒
        // ActorAsked(info: ActorInfo, message: String)
        builder.setType(ProtoAnnotationType.ACTOR_ASKED)
        dataBuilder.setInfo1(toProtoActorInfo(a.info))
        dataBuilder.setString1(a.message)

      case a: ActorFailed ⇒
        // ActorFailed(info: ActorInfo, reason: String, supervisor: ActorInfo)
        builder.setType(ProtoAnnotationType.ACTOR_FAILED)
        dataBuilder.setInfo1(toProtoActorInfo(a.info))
        dataBuilder.setString1(a.reason)
        dataBuilder.setInfo2(toProtoActorInfo(a.supervisor))

      case a: TempActorCreated ⇒
        // TempActorCreated(info: ActorInfo)
        builder.setType(ProtoAnnotationType.TEMP_ACTOR_CREATED)
        dataBuilder.setInfo1(toProtoActorInfo(a.info))

      case a: TempActorStopped ⇒
        // TempActorStopped(info: ActorInfo)
        builder.setType(ProtoAnnotationType.TEMP_ACTOR_STOPPED)
        dataBuilder.setInfo1(toProtoActorInfo(a.info))

      case a: ActorSelectionTold ⇒
        // ActorSelectionTold(info: ActorSelectionInfo, message: String, sender: Option[ActorInfo])
        builder.setType(ProtoAnnotationType.ACTOR_SELECTION_TOLD)
        dataBuilder.setInfo1(toProtoActorSelectionInfo(a.info))
        dataBuilder.setString1(a.message)
        for (sender ← a.sender) dataBuilder.setInfo2(toProtoActorInfo(sender))

      case a: ActorSelectionAsked ⇒
        // ActorSelectionAsked(info: ActorSelectionInfo, message: String)
        builder.setType(ProtoAnnotationType.ACTOR_SELECTION_ASKED)
        dataBuilder.setInfo1(toProtoActorSelectionInfo(a.info))
        dataBuilder.setString1(a.message)

      case a: FutureCreated ⇒
        // FutureCreated(info: FutureInfo)
        builder.setType(ProtoAnnotationType.FUTURE_CREATED)
        dataBuilder.setInfo1(toProtoFutureInfo(a.info))

      case a: FutureScheduled ⇒
        // FutureScheduled(info: FutureInfo, taskInfo: TaskInfo)
        builder.setType(ProtoAnnotationType.FUTURE_SCHEDULED)
        dataBuilder.setInfo1(toProtoFutureInfo(a.info))
        dataBuilder.setInfo2(toProtoTaskInfo(a.taskInfo))

      case a: FutureSucceeded ⇒
        // FutureSucceeded(info: FutureInfo, result: String, resultInfo: Info)
        builder.setType(ProtoAnnotationType.FUTURE_SUCCEEDED)
        dataBuilder.setInfo1(toProtoFutureInfo(a.info))
        dataBuilder.setString1(a.result)
        dataBuilder.setInfo2(toProtoInfo(a.resultInfo))

      case a: FutureFailed ⇒
        // FutureFailed(info: FutureInfo, exception: String)
        builder.setType(ProtoAnnotationType.FUTURE_FAILED)
        dataBuilder.setInfo1(toProtoFutureInfo(a.info))
        dataBuilder.setString1(a.exception)

      case a: FutureCallbackAdded ⇒
        // FutureCallbackAdded(info: FutureInfo)
        builder.setType(ProtoAnnotationType.FUTURE_CALLBACK_ADDED)
        dataBuilder.setInfo1(toProtoFutureInfo(a.info))

      case a: FutureCallbackStarted ⇒
        // FutureCallbackStarted(info: FutureInfo)
        builder.setType(ProtoAnnotationType.FUTURE_CALLBACK_STARTED)
        dataBuilder.setInfo1(toProtoFutureInfo(a.info))

      case a: FutureCallbackCompleted ⇒
        // FutureCallbackCompleted(info: FutureInfo)
        builder.setType(ProtoAnnotationType.FUTURE_CALLBACK_COMPLETED)
        dataBuilder.setInfo1(toProtoFutureInfo(a.info))

      case a: FutureAwaited ⇒
        // FutureAwaited(info: FutureInfo)
        builder.setType(ProtoAnnotationType.FUTURE_AWAITED)
        dataBuilder.setInfo1(toProtoFutureInfo(a.info))

      case a: FutureAwaitTimedOut ⇒
        // FutureAwaitTimedOut(info: FutureInfo, duration: String)
        builder.setType(ProtoAnnotationType.FUTURE_AWAIT_TIMED_OUT)
        dataBuilder.setInfo1(toProtoFutureInfo(a.info))
        dataBuilder.setString1(a.duration)

      case a: RunnableScheduled ⇒
        // RunnableScheduled(info: TaskInfo)
        builder.setType(ProtoAnnotationType.RUNNABLE_SCHEDULED)
        dataBuilder.setInfo1(toProtoTaskInfo(a.info))

      case a: RunnableStarted ⇒
        // RunnableStarted(info: TaskInfo)
        builder.setType(ProtoAnnotationType.RUNNABLE_STARTED)
        dataBuilder.setInfo1(toProtoTaskInfo(a.info))

      case a: RunnableCompleted ⇒
        // RunnableCompleted(info: TaskInfo)
        builder.setType(ProtoAnnotationType.RUNNABLE_COMPELETED)
        dataBuilder.setInfo1(toProtoTaskInfo(a.info))

      case a: ScheduledOnce ⇒
        // ScheduledOnce(info: TaskInfo, delay: String)
        builder.setType(ProtoAnnotationType.SCHEDULED_ONCE)
        dataBuilder.setInfo1(toProtoTaskInfo(a.info))
        dataBuilder.setString1(a.delay)

      case a: ScheduledStarted ⇒
        // ScheduledStarted(info: TaskInfo)
        builder.setType(ProtoAnnotationType.SCHEDULED_STARTED)
        dataBuilder.setInfo1(toProtoTaskInfo(a.info))

      case a: ScheduledCompleted ⇒
        // ScheduledCompleted(info: TaskInfo)
        builder.setType(ProtoAnnotationType.SCHEDULED_COMPLETED)
        dataBuilder.setInfo1(toProtoTaskInfo(a.info))

      case a: ScheduledCancelled ⇒
        // ScheduledCancelled(info: TaskInfo)
        builder.setType(ProtoAnnotationType.SCHEDULED_CANCELLED)
        dataBuilder.setInfo1(toProtoTaskInfo(a.info))

      case a: RemoteMessageSent ⇒
        // RemoteMessageSent(info: ActorInfo, message: String, messageSize: Int)
        builder.setType(ProtoAnnotationType.REMOTE_MESSAGE_SENT)
        dataBuilder.setInfo1(toProtoActorInfo(a.info))
        dataBuilder.setString1(a.message)
        dataBuilder.setNumber(a.messageSize)

      case a: RemoteMessageReceived ⇒
        // RemoteMessageReceived(info: ActorInfo, message: String, messageSize: Int)
        builder.setType(ProtoAnnotationType.REMOTE_MESSAGE_RECEIVED)
        dataBuilder.setInfo1(toProtoActorInfo(a.info))
        dataBuilder.setString1(a.message)
        dataBuilder.setNumber(a.messageSize)

      case a: RemoteMessageCompleted ⇒
        // RemoteMessageCompleted(info: ActorInfo, message: String)
        builder.setType(ProtoAnnotationType.REMOTE_MESSAGE_COMPLETED)
        dataBuilder.setInfo1(toProtoActorInfo(a.info))
        dataBuilder.setString1(a.message)

      case a: RemoteStatus ⇒
        // RemoteStatus(statusType: RemoteStatusType.Value, serverNode: Option[String], clientNode: Option[String], cause: Option[String])
        builder.setType(ProtoAnnotationType.REMOTE_STATUS)
        dataBuilder.setString1(a.statusType.toString)
        a.serverNode foreach dataBuilder.setString2
        a.clientNode foreach dataBuilder.setString3
        a.cause foreach dataBuilder.setString4

      case a: RemotingLifecycle ⇒
        // RemotingLifecycle(eventType: RemotingLifecycleEventType.Value, localAddress: Option[String],
        //   remoteAddress: Option[String], inbound: Option[Boolean], cause: Option[String], listenAddresses: Set[String])
        builder.setType(ProtoAnnotationType.REMOTING_LIFECYCLE)
        dataBuilder.setString1(a.eventType.toString)
        a.localAddress foreach dataBuilder.setString2
        a.remoteAddress foreach dataBuilder.setString3
        a.inbound foreach dataBuilder.setBool1
        a.cause foreach dataBuilder.setString4
        a.listenAddresses foreach dataBuilder.addStrings

      case a: EventStreamError ⇒
        // EventStreamError(message: String)
        builder.setType(ProtoAnnotationType.EVENT_STREAM_ERROR)
        dataBuilder.setString1(a.message)

      case a: EventStreamWarning ⇒
        // EventStreamWarning(message: String)
        builder.setType(ProtoAnnotationType.EVENT_STREAM_WARNING)
        dataBuilder.setString1(a.message)

      case a: EventStreamDeadLetter ⇒
        // EventStreamDeadLetter(message: String, sender: ActorInfo, recipient: ActorInfo)
        builder.setType(ProtoAnnotationType.EVENT_STREAM_DEAD_LETTER)
        dataBuilder.setString1(a.message)
        dataBuilder.setInfo1(toProtoActorInfo(a.sender))
        dataBuilder.setInfo2(toProtoActorInfo(a.recipient))

      case a: EventStreamUnhandledMessage ⇒
        // EventStreamUnhandledMessage(message: String, sender: ActorInfo, recipient: ActorInfo)
        builder.setType(ProtoAnnotationType.EVENT_STREAM_UNHANDLED_MESSAGE)
        dataBuilder.setString1(a.message)
        dataBuilder.setInfo1(toProtoActorInfo(a.sender))
        dataBuilder.setInfo2(toProtoActorInfo(a.recipient))

      case a: DispatcherStatus ⇒
        // DispatcherStatus(dispatcher: String, dispatcherType: String, metrics: DispatcherMetrics)
        builder.setType(ProtoAnnotationType.DISPATCHER_STATUS)
        dataBuilder.setString1(a.dispatcher)
        dataBuilder.setString2(a.dispatcherType)
        dataBuilder.setDispatcherMetrics(toProtoDispatcherMetrics(a.metrics))

      case a: SystemMetrics ⇒
        // see Annotation.scala
        builder.setType(ProtoAnnotationType.SYSTEM_METRICS)
        dataBuilder.setSystemMetrics(toProtoSystemMetrics(a))

      case a: DeadlockedThreads ⇒
        // DeadlockedThreads(threads: Set[String])
        builder.setType(ProtoAnnotationType.DEADLOCKED_THREADS)
        dataBuilder.setDeadlockedThreads(toProtoDeadlockedThreads(a.message, a.deadlocks))

      case a: ActionRouteRequest ⇒
        builder.setType(ProtoAnnotationType.ACTION_ROUTE_REQUEST)
        dataBuilder.setPlayActionInfo(toProtoActionInfo(requestInfo = Some(toProtoActionRequestInfo(a.requestInfo)), routeRequestResult = Some(a.result)))

      case a: ActionError ⇒
        builder.setType(ProtoAnnotationType.ACTION_ERROR)
        dataBuilder.setString1(a.message)
        dataBuilder.setPlayActionInfo(toProtoActionInfo(requestInfo = Some(toProtoActionRequestInfo(a.requestInfo)), stackTrace = Some(a.stackTrace)))

      case a: ActionHandlerNotFound ⇒
        builder.setType(ProtoAnnotationType.ACTION_HANDLER_NOT_FOUND)
        dataBuilder.setPlayActionInfo(toProtoActionInfo(requestInfo = Some(toProtoActionRequestInfo(a.requestInfo))))

      case a: ActionBadRequest ⇒
        builder.setType(ProtoAnnotationType.ACTION_BAD_REQUEST)
        dataBuilder.setString1(a.error)
        dataBuilder.setPlayActionInfo(toProtoActionInfo(requestInfo = Some(toProtoActionRequestInfo(a.requestInfo))))

      case a: ActionInvoked ⇒
        builder.setType(ProtoAnnotationType.ACTION_INVOKED)
        dataBuilder.setPlayActionInfo(toProtoActionInfo(invocationInfo = Some(toProtoActionInvocationInfo(a.invocationInfo))))

      case a: ActionResolved ⇒
        builder.setType(ProtoAnnotationType.ACTION_RESOLVED)
        dataBuilder.setPlayActionInfo(toProtoActionInfo(resolutionInfo = Some(toProtoActionResolvedInfo(a.resolutionInfo))))

      case ActionResultGenerationStart ⇒
        builder.setType(ProtoAnnotationType.ACTION_RESULT_GENERATION_START)
        dataBuilder.setPlayActionInfo(toProtoActionInfo())

      case ActionChunkedInputStart ⇒
        builder.setType(ProtoAnnotationType.ACTION_CHUNKED_INPUT_START)
        dataBuilder.setPlayActionInfo(toProtoActionInfo())

      case ActionChunkedInputEnd ⇒
        builder.setType(ProtoAnnotationType.ACTION_CHUNKED_INPUT_END)
        dataBuilder.setPlayActionInfo(toProtoActionInfo())

      case ActionResultGenerationEnd ⇒
        builder.setType(ProtoAnnotationType.ACTION_RESULT_GENERATION_END)
        dataBuilder.setPlayActionInfo(toProtoActionInfo())

      case a: ActionChunkedResult ⇒
        builder.setType(ProtoAnnotationType.ACTION_CHUNKED_RESULT)
        dataBuilder.setPlayActionInfo(toProtoActionInfo(resultInfo = Some(toProtoActionResultInfo(a.resultInfo))))

      case a: ActionSimpleResult ⇒
        builder.setType(ProtoAnnotationType.ACTION_SIMPLE_RESULT)
        dataBuilder.setPlayActionInfo(toProtoActionInfo(resultInfo = Some(toProtoActionResultInfo(a.resultInfo))))

      case ActionAsyncResult ⇒
        builder.setType(ProtoAnnotationType.ACTION_ASYNC_RESULT)
        dataBuilder.setPlayActionInfo(toProtoActionInfo())

      case a: IterateeCreated ⇒
        builder.setType(ProtoAnnotationType.ITERATEE_CREATED)
        dataBuilder.setPlayIterateeInfo(toProtoIterateeInfo(info = a.info))

      case a: IterateeFolded ⇒
        builder.setType(ProtoAnnotationType.ITERATEE_FOLDED)
        dataBuilder.setPlayIterateeInfo(toProtoIterateeInfo(info = a.info))

      case a: IterateeDone ⇒
        builder.setType(ProtoAnnotationType.ITERATEE_DONE)
        dataBuilder.setPlayIterateeInfo(toProtoIterateeInfo(info = a.info))

      case a: IterateeError ⇒
        builder.setType(ProtoAnnotationType.ITERATEE_ERROR)
        dataBuilder.setPlayIterateeInfo(toProtoIterateeInfo(info = a.info))

      case a: IterateeContinued ⇒
        builder.setType(ProtoAnnotationType.ITERATEE_CONTINUED)
        dataBuilder.setPlayIterateeInfo(toProtoIterateeInfo(info = a.info, input = Some(a.input), nextInfo = Some(a.nextInfo)))

      case NettyHttpReceivedStart ⇒
        builder.setType(ProtoAnnotationType.NETTY_HTTP_RECEIVED_START)
        dataBuilder.setPlayNettyInfo(toProtoNettyInfo())

      case NettyHttpReceivedEnd ⇒
        builder.setType(ProtoAnnotationType.NETTY_HTTP_RECEIVED_END)
        dataBuilder.setPlayNettyInfo(toProtoNettyInfo())

      case NettyPlayReceivedStart ⇒
        builder.setType(ProtoAnnotationType.NETTY_PLAY_RECEIVED_START)
        dataBuilder.setPlayNettyInfo(toProtoNettyInfo())

      case NettyPlayReceivedEnd ⇒
        builder.setType(ProtoAnnotationType.NETTY_PLAY_RECEIVED_END)
        dataBuilder.setPlayNettyInfo(toProtoNettyInfo())

      case a: NettyResponseHeader ⇒
        builder.setType(ProtoAnnotationType.NETTY_RESPONSE_HEADER)
        dataBuilder.setPlayNettyInfo(toProtoNettyInfo(size = Some(a.size)))

      case a: NettyResponseBody ⇒
        builder.setType(ProtoAnnotationType.NETTY_RESPONSE_BODY)
        dataBuilder.setPlayNettyInfo(toProtoNettyInfo(size = Some(a.size)))

      case a: NettyWriteChunk ⇒
        builder.setType(ProtoAnnotationType.NETTY_WRITE_CHUNK)
        dataBuilder.setPlayNettyInfo(toProtoNettyInfo(overhead = Some(a.overhead), size = Some(a.size)))

      case a: NettyReadBytes ⇒
        builder.setType(ProtoAnnotationType.NETTY_READ_BYTES)
        dataBuilder.setPlayNettyInfo(toProtoNettyInfo(size = Some(a.size)))
    }

    builder.setData(dataBuilder.build())
    builder.build()
  }

  private[util] def toProtoSystemMessage(message: SysMsg, builder: com.typesafe.trace.TraceProtocol.ProtoAnnotationData.Builder) = message match {
    case CreateSysMsg ⇒
      builder.setSysMsg(ProtoSystemMessageType.SYS_MSG_CREATE)

    case SuspendSysMsg ⇒
      builder.setSysMsg(ProtoSystemMessageType.SYS_MSG_SUSPEND)

    case ResumeSysMsg ⇒
      builder.setSysMsg(ProtoSystemMessageType.SYS_MSG_RESUME)

    case TerminateSysMsg ⇒
      builder.setSysMsg(ProtoSystemMessageType.SYS_MSG_TERMINATE)

    case a: RecreateSysMsg ⇒
      // RecreateSysMsg(cause: String)
      builder.setSysMsg(ProtoSystemMessageType.SYS_MSG_RECREATE)
      builder.setString1(a.cause)

    case a: SuperviseSysMsg ⇒
      // SuperviseSysMsg(child: ActorInfo)
      builder.setSysMsg(ProtoSystemMessageType.SYS_MSG_SUPERVISE)
      builder.setInfo2(toProtoActorInfo(a.child))

    case a: ChildTerminatedSysMsg ⇒
      // ChildTerminatedSysMsg(child: ActorInfo)
      builder.setSysMsg(ProtoSystemMessageType.SYS_MSG_CHILD_TERMINATED)
      builder.setInfo2(toProtoActorInfo(a.child))

    case a: LinkSysMsg ⇒
      // LinkSysMsg(subject: ActorInfo)
      builder.setSysMsg(ProtoSystemMessageType.SYS_MSG_LINK)
      builder.setInfo2(toProtoActorInfo(a.subject))

    case a: UnlinkSysMsg ⇒
      // UnlinkSysMsg(subject: ActorInfo)
      builder.setSysMsg(ProtoSystemMessageType.SYS_MSG_UNLINK)
      builder.setInfo2(toProtoActorInfo(a.subject))

    case a: WatchSysMsg ⇒
      // WatchSysMsg(watchee: ActorInfo, watcher: ActorInfo)
      builder.setSysMsg(ProtoSystemMessageType.SYS_MSG_WATCH)
      builder.setInfo2(toProtoActorInfo(a.watchee))
      builder.setInfo3(toProtoActorInfo(a.watcher))

    case a: UnwatchSysMsg ⇒
      // UnwatchSysMsg(watchee: ActorInfo, watcher: ActorInfo)
      builder.setSysMsg(ProtoSystemMessageType.SYS_MSG_UNWATCH)
      builder.setInfo2(toProtoActorInfo(a.watchee))
      builder.setInfo3(toProtoActorInfo(a.watcher))

    case NoMessageSysMsg ⇒
      builder.setSysMsg(ProtoSystemMessageType.SYS_MSG_NO_MESSAGE)

    case a: FailedSysMsg ⇒
      // FailedSysMsg(child: ActorInfo, cause: String)
      builder.setSysMsg(ProtoSystemMessageType.SYS_MSG_FAILED)
      builder.setInfo2(toProtoActorInfo(a.child))
      builder.setString1(a.cause)

    case a: DeathWatchSysMsg ⇒
      // DeathWatchSysMsg(actor: ActorInfo, existenceConfirmed: Boolean, addressTerminated: Boolean)
      builder.setSysMsg(ProtoSystemMessageType.SYS_MSG_DEATH_WATCH)
      builder.setInfo2(toProtoActorInfo(a.actor))
      builder.setBool1(a.existenceConfirmed)
      builder.setBool2(a.addressTerminated)
  }

  private[util] def toProtoInfo(info: Info): ProtoInfo = info match {
    case i: ActorInfo    ⇒ toProtoActorInfo(i)
    case i: FutureInfo   ⇒ toProtoFutureInfo(i)
    case i: TaskInfo     ⇒ toProtoTaskInfo(i)
    case i: IterateeInfo ⇒ toProtoIterateeInfo2(i)
    case _               ⇒ toProtoNoInfo()
  }

  private[util] def toProtoNoInfo(): ProtoInfo = {
    val builder = ProtoInfo.newBuilder
    builder.setType(ProtoInfoType.NO_INFO)
    builder.build()
  }

  private[util] def toProtoActorInfo(info: ActorInfo): ProtoInfo = {
    // ActorInfo(path: String, dispatcher: Option[String], remote: Boolean, router: Boolean, tags: Set[String])
    val builder = ProtoInfo.newBuilder
    builder.setType(ProtoInfoType.ACTOR_INFO)
    setProtoActorInfoFields(builder, info)
    builder.build()
  }

  private[util] def setProtoActorInfoFields(builder: ProtoInfo.Builder, info: ActorInfo): ProtoInfo.Builder = {
    builder.setPath(info.path)
    info.dispatcher foreach builder.setDispatcher
    builder.setRemote(info.remote)
    builder.setRouter(info.router)
    info.tags foreach builder.addTags
    builder
  }

  private[util] def toProtoActorSelectionInfo(info: ActorSelectionInfo): ProtoInfo = {
    // ActorSelectionInfo(anchor: ActorInfo, path: String)
    val builder = ProtoInfo.newBuilder
    builder.setType(ProtoInfoType.ACTOR_SELECTION_INFO)
    setProtoActorInfoFields(builder, info.anchor)
    builder.setPath2(info.path)
    builder.build()
  }

  private[util] def toProtoFutureInfo(info: FutureInfo): ProtoInfo = {
    // FutureInfo(uuid: UUID, dispatcher: String)
    val builder = ProtoInfo.newBuilder
    builder.setType(ProtoInfoType.FUTURE_INFO)
    builder.setUuid(toProtoUuid(info.uuid))
    builder.setDispatcher(info.dispatcher)
    builder.build()
  }

  private[util] def toProtoIterateeInfo2(info: IterateeInfo): ProtoInfo = {
    // IterateeInfo(uuid: UUID)
    val builder = ProtoInfo.newBuilder
    builder.setType(ProtoInfoType.ITERATEE_INFO)
    builder.setUuid(toProtoUuid(info.uuid))
    builder.build()
  }

  private[util] def toProtoActionInfo(invocationInfo: Option[ProtoPlayActionInvocationInfo] = None,
                                      resultInfo: Option[ProtoPlayActionResultInfo] = None,
                                      resolutionInfo: Option[ProtoPlayActionResolvedInfo] = None,
                                      requestInfo: Option[ProtoPlayActionRequestInfo] = None,
                                      routeRequestResult: Option[ActionRouteRequestResult] = None,
                                      stackTrace: Option[Seq[String]] = None): ProtoPlayActionInfo = {
    val builder = ProtoPlayActionInfo.newBuilder
    invocationInfo.foreach(builder.setInvocationInfo)
    resultInfo.foreach(builder.setResultInfo)
    resolutionInfo.foreach(builder.setResolvedInfo)
    requestInfo.foreach(builder.setRequestInfo)
    routeRequestResult.foreach(_ match {
      case ActionRouteRequestResult.NoHandler       ⇒ builder.setRouteRequestResult(ProtoActionRouteRequestResult.NO_HANDLER)
      case ActionRouteRequestResult.EssentialAction ⇒ builder.setRouteRequestResult(ProtoActionRouteRequestResult.ESSENTIAL_ACTION)
      case ActionRouteRequestResult.WebSocket       ⇒ builder.setRouteRequestResult(ProtoActionRouteRequestResult.WEB_SOCKET)
    })
    stackTrace.foreach(_ foreach builder.addStackTrace)
    builder.build()
  }

  private[util] def toProtoIterateeInfo(info: IterateeInfo,
                                        input: Option[IterateeInput.Tag] = None,
                                        nextInfo: Option[IterateeInfo] = None): ProtoPlayIterateeInfo = {
    val builder = ProtoPlayIterateeInfo.newBuilder
    builder.setUuid(toProtoUuid(info.uuid))
    input.foreach(_ match {
      case IterateeInput.El(value) ⇒
        builder.setTag(IterateeInput.ElTag)
        builder.setValue(value)
      case i ⇒ builder.setTag(i.tag)
    })
    nextInfo.foreach(i ⇒ builder.setNextUuid(toProtoUuid(i.uuid)))
    builder.build()
  }

  private[util] def toProtoNettyInfo(size: Option[Int] = None, overhead: Option[Int] = None): ProtoPlayNettyInfo = {
    val builder = ProtoPlayNettyInfo.newBuilder
    overhead.foreach(builder.setOverhead)
    size.foreach(builder.setSize)
    builder.build()
  }

  private[util] def toProtoStringStringMapEntry(key: String, value: String): ProtoStringStringMapEntry = {
    val builder = ProtoStringStringMapEntry.newBuilder
    builder.setKey(key)
    builder.setValue(value)
    builder.build()
  }

  private[util] def toProtoStringSeqStringMapEntry(key: String, values: Seq[String]): ProtoStringSeqStringMapEntry = {
    val builder = ProtoStringSeqStringMapEntry.newBuilder
    builder.setKey(key)
    values foreach builder.addValues
    builder.build()
  }

  private[util] def toProtoActionRequestInfo(info: ActionRequestInfo): ProtoPlayActionRequestInfo = {
    val builder = ProtoPlayActionRequestInfo.newBuilder
    builder.setId(info.id)
    info.tags.map { case (k, v) ⇒ toProtoStringStringMapEntry(k, v) }.foreach(builder.addTags)
    builder.setUri(info.uri)
    builder.setPath(info.path)
    builder.setMethod(info.method)
    builder.setVersion(info.version)
    info.queryString.map { case (k, vs) ⇒ toProtoStringSeqStringMapEntry(k, vs) }.foreach(builder.addQueryString)
    info.headers.map { case (k, vs) ⇒ toProtoStringSeqStringMapEntry(k, vs) }.foreach(builder.addHeaders)
    builder.build()
  }

  private[util] def toProtoActionInvocationInfo(info: ActionInvocationInfo): ProtoPlayActionInvocationInfo = {
    val builder = ProtoPlayActionInvocationInfo.newBuilder
    builder.setPattern(info.pattern)
    builder.setController(info.controller)
    builder.setMethod(info.method)
    builder.setId(info.id)
    builder.setUri(info.uri)
    builder.setPath(info.path)
    builder.setHttpMethod(info.httpMethod)
    builder.setVersion(info.version)
    builder.setRemoteAddress(info.remoteAddress)
    info.host.foreach(builder.setHost(_))
    info.domain.foreach(builder.setDomain(_))
    info.session.foreach(_.map { case (k, vs) ⇒ toProtoStringStringMapEntry(k, vs) }.foreach(builder.addSession))
    builder.build()
  }

  private[util] def toProtoActionResolvedInfo(info: ActionResolvedInfo): ProtoPlayActionResolvedInfo = {
    val builder = ProtoPlayActionResolvedInfo.newBuilder
    builder.setPath(info.path)
    builder.setController(info.controller)
    builder.setMethod(info.method)
    builder.setVerb(info.verb)
    builder.setComments(info.comments)
    info.parameterTypes foreach builder.addParameterTypes
    builder.build()
  }

  private[util] def toProtoActionResultInfo(info: ActionResultInfo): ProtoPlayActionResultInfo = {
    val builder = ProtoPlayActionResultInfo.newBuilder
    builder.setHttpResponseCode(info.httpResponseCode)
    builder.build()
  }

  private[util] def toProtoActionResult(r: ActionResponseAnnotation): ProtoPlayActionResult = {
    val builder = ProtoPlayActionResult.newBuilder
    builder.setResultInfo(toProtoActionResultInfo(r.resultInfo))
    r match {
      case _: ActionSimpleResult  ⇒ builder.setType(ProtoAnnotationType.ACTION_SIMPLE_RESULT)
      case _: ActionChunkedResult ⇒ builder.setType(ProtoAnnotationType.ACTION_CHUNKED_RESULT)
    }
    builder.build()
  }

  private[util] def toProtoTaskInfo(info: TaskInfo): ProtoInfo = {
    // TaskInfo(uuid: UUID, dispatcher: String)
    val builder = ProtoInfo.newBuilder
    builder.setType(ProtoInfoType.TASK_INFO)
    builder.setUuid(toProtoUuid(info.uuid))
    builder.setDispatcher(info.dispatcher)
    builder.build()
  }

  private[util] def toProtoDispatcherMetrics(metrics: DispatcherMetrics): ProtoDispatcherMetrics = {
    val builder = ProtoDispatcherMetrics.newBuilder
    builder.setCorePoolSize(metrics.corePoolSize)
    builder.setMaximumPoolSize(metrics.maximumPoolSize)
    builder.setKeepAliveTime(metrics.keepAliveTime)
    builder.setRejectedHandler(metrics.rejectedHandler)
    builder.setActiveThreadCount(metrics.activeThreadCount)
    builder.setTaskCount(metrics.taskCount)
    builder.setCompletedTaskCount(metrics.completedTaskCount)
    builder.setLargestPoolSize(metrics.largestPoolSize)
    builder.setPoolSize(metrics.poolSize)
    builder.setQueueSize(metrics.queueSize)
    builder.build()
  }

  private[util] def toProtoSystemMetrics(metrics: SystemMetrics): ProtoSystemMetrics = {
    val builder = ProtoSystemMetrics.newBuilder
    builder.setRunningActors(metrics.runningActors)
    builder.setStartTime(metrics.startTime)
    builder.setUpTime(metrics.upTime)
    builder.setAvailableProcessors(metrics.availableProcessors)
    builder.setDaemonThreadCount(metrics.daemonThreadCount)
    builder.setThreadCount(metrics.threadCount)
    builder.setPeakThreadCount(metrics.peakThreadCount)
    builder.setCommittedHeap(metrics.committedHeap)
    builder.setMaxHeap(metrics.maxHeap)
    builder.setUsedHeap(metrics.usedHeap)
    builder.setCommittedNonHeap(metrics.committedNonHeap)
    builder.setMaxNonHeap(metrics.maxNonHeap)
    builder.setUsedNonHeap(metrics.usedNonHeap)
    builder.setGcCountPerMinute(metrics.gcCountPerMinute)
    builder.setGcTimePercent(metrics.gcTimePercent)
    builder.setSystemLoadAverage(metrics.systemLoadAverage)
    for (additional ← metrics.additional) builder.setAdditional(toProtoAdditionalSystemMetrics(additional))
    builder.build()
  }

  private[util] def toProtoAdditionalSystemMetrics(metrics: AdditionalSystemMetrics): ProtoAdditionalSystemMetrics = {
    val builder = ProtoAdditionalSystemMetrics.newBuilder
    builder.setCpuUser(metrics.cpu.cpuUser)
    builder.setCpuSys(metrics.cpu.cpuSys)
    builder.setCpuCombined(metrics.cpu.cpuCombined)
    builder.setPidCpu(metrics.cpu.pidCpu)
    builder.setLoadAverage1Min(metrics.cpu.loadAverage1min)
    builder.setLoadAverage5Min(metrics.cpu.loadAverage5min)
    builder.setLoadAverage15Min(metrics.cpu.loadAverage15min)
    builder.setContextSwitches(metrics.cpu.contextSwitches)
    builder.setMemUsage(metrics.memory.memUsage)
    builder.setMemSwapPageIn(metrics.memory.memSwapPageIn)
    builder.setMemSwapPageOut(metrics.memory.memSwapPageOut)
    builder.setTcpCurrEstab(metrics.network.tcpCurrEstab)
    builder.setTcpEstabResets(metrics.network.tcpEstabResets)
    builder.setNetRxBytesRate(metrics.network.netRxBytesRate)
    builder.setNetTxBytesRate(metrics.network.netTxBytesRate)
    builder.setNetRxErrors(metrics.network.netRxErrors)
    builder.setNetTxErrors(metrics.network.netTxErrors)
    builder.build()
  }

  private[util] def toProtoDeadlockedThreads(message: String, deadlocks: Set[String]): ProtoDeadlockedThreads = {
    val builder = ProtoDeadlockedThreads.newBuilder
    builder.setMessage(message)
    deadlocks foreach builder.addDeadlocks
    builder.build()
  }

  def toProtoUuid(uuid: UUID): ProtoUuid = {
    ProtoUuid.newBuilder.setHigh(uuid.time).setLow(uuid.clockSeqAndNode).build()
  }

  def toProtoTraceContext(context: TraceContext): ProtoTraceContext = {
    ProtoTraceContext.newBuilder
      .setTrace(toProtoUuid(context.trace))
      .setParent(toProtoUuid(context.parent))
      .setSampled(context.sampled)
      .build()
  }

  // =========================================================================
  // Converting from protobuf
  // =========================================================================

  def fromProto(batch: ProtoBatch): Batch = {
    val traceEvents = fromProtoTraceEvents(batch.getTraceEventsList)
    Batch(traceEvents)
  }

  private[util] def fromProtoTraceEvents(traceEvents: java.util.List[ProtoTraceEvents]): Seq[TraceEvents] = {
    // Not nice to use Java List directly - but faster than converting to Scala's collections
    var indexOuter = 0
    val traceEventsBuffer = new ListBuffer[TraceEvents]()
    while (indexOuter < traceEvents.size) {
      var indexInner = 0
      val traceEventBuffer = new ListBuffer[TraceEvent]()
      val events = traceEvents.get(indexOuter).getEventList
      while (indexInner < events.size) {
        traceEventBuffer += fromProtoTraceEvent(events.get(indexInner))
        indexInner += 1
      }
      traceEventsBuffer += TraceEvents(traceEventBuffer.toSeq)
      indexOuter += 1
    }
    traceEventsBuffer.toSeq
  }

  private[util] def fromProtoTraceEvent(event: ProtoTraceEvent): TraceEvent = {
    val annotation = fromProtoAnnotation(event.getAnnotation)
    TraceEvent(annotation,
      fromProtoUuid(event.getId),
      fromProtoUuid(event.getTrace),
      fromProtoUuid(event.getLocal),
      fromProtoUuid(event.getParent),
      event.getSampled,
      event.getNode,
      event.getHost,
      event.getActorSystem,
      event.getTimestamp,
      event.getNanoTime)
  }

  private[util] def fromProtoAnnotation(proto: ProtoAnnotation): Annotation = {
    val data = proto.getData

    proto.getType match {

      case ProtoAnnotationType.GROUP_STARTED ⇒
        // GroupStarted(name: String)
        GroupStarted(data.getString1)

      case ProtoAnnotationType.GROUP_ENDED ⇒
        // GroupEnded(name: String)
        GroupEnded(data.getString1)

      case ProtoAnnotationType.MARKER_STARTED ⇒
        // MarkerStarted(name: String)
        MarkerStarted(data.getString1)

      case ProtoAnnotationType.MARKER ⇒
        // Marker(name: String, data: String)
        Marker(data.getString1, data.getString2)

      case ProtoAnnotationType.MARKER_ENDED ⇒
        // MarkerEnded(name: String)
        MarkerEnded(data.getString1)

      case ProtoAnnotationType.SYSTEM_STARTED ⇒
        // SystemStarted(startTime: Long)
        SystemStarted(data.getNumber)

      case ProtoAnnotationType.SYSTEM_SHUTDOWN ⇒
        // SystemShutdown(shutdownTime: Long)
        SystemShutdown(data.getNumber)

      case ProtoAnnotationType.TOP_LEVEL_ACTOR_REQUESTED ⇒
        // TopLevelActorRequested(guardian: ActorInfo, name: String)
        TopLevelActorRequested(fromProtoActorInfo(data.getInfo1), data.getString1)

      case ProtoAnnotationType.TOP_LEVEL_ACTOR_CREATED ⇒
        // TopLevelActorCreated(info: ActorInfo)
        TopLevelActorCreated(fromProtoActorInfo(data.getInfo1))

      case ProtoAnnotationType.SYSTEM_MESSAGE_DISPATCHED ⇒
        // SysMsgDispatched(info: ActorInfo, message: SysMsg)
        SysMsgDispatched(fromProtoActorInfo(data.getInfo1), fromProtoSystemMessage(data))

      case ProtoAnnotationType.SYSTEM_MESSAGE_RECEIVED ⇒
        // SysMsgReceived(info: ActorInfo, message: SysMsg)
        SysMsgReceived(fromProtoActorInfo(data.getInfo1), fromProtoSystemMessage(data))

      case ProtoAnnotationType.SYSTEM_MESSAGE_COMPLETED ⇒
        // SysMsgCompleted(info: ActorInfo, message: SysMsg)
        SysMsgCompleted(fromProtoActorInfo(data.getInfo1), fromProtoSystemMessage(data))

      case ProtoAnnotationType.ACTOR_REQUESTED ⇒
        // ActorRequested(info: ActorInfo, actor: ActorInfo)
        ActorRequested(fromProtoActorInfo(data.getInfo1), fromProtoActorInfo(data.getInfo2))

      case ProtoAnnotationType.ACTOR_CREATED ⇒
        // ActorCreated(info: ActorInfo)
        ActorCreated(fromProtoActorInfo(data.getInfo1))

      case ProtoAnnotationType.ACTOR_TOLD ⇒
        // ActorTold(info: ActorInfo, message: String, sender: Option[ActorInfo])
        val sender = if (data.hasInfo2) Some(fromProtoActorInfo(data.getInfo2)) else None
        ActorTold(fromProtoActorInfo(data.getInfo1), data.getString1, sender)

      case ProtoAnnotationType.ACTOR_AUTO_RECEIVED ⇒
        // ActorAutoReceived(info: ActorInfo, message: String)
        ActorAutoReceived(fromProtoActorInfo(data.getInfo1), data.getString1)

      case ProtoAnnotationType.ACTOR_AUTO_COMPLETED ⇒
        // ActorAutoCompleted(info: ActorInfo, message: String)
        ActorAutoCompleted(fromProtoActorInfo(data.getInfo1), data.getString1)

      case ProtoAnnotationType.ACTOR_RECEIVED ⇒
        // ActorReceived(info: ActorInfo, message: String)
        ActorReceived(fromProtoActorInfo(data.getInfo1), data.getString1)

      case ProtoAnnotationType.ACTOR_COMPLETED ⇒
        // ActorCompleted(info: ActorInfo, message: String)
        ActorCompleted(fromProtoActorInfo(data.getInfo1), data.getString1)

      case ProtoAnnotationType.ACTOR_ASKED ⇒
        // ActorAsked(info: ActorInfo, message: String)
        ActorAsked(fromProtoActorInfo(data.getInfo1), data.getString1)

      case ProtoAnnotationType.ACTOR_FAILED ⇒
        // ActorFailed(info: ActorInfo, reason: String, supervisor: ActorInfo)
        ActorFailed(fromProtoActorInfo(data.getInfo1), data.getString1, fromProtoActorInfo(data.getInfo2))

      case ProtoAnnotationType.TEMP_ACTOR_CREATED ⇒
        // TempActorCreated(info: ActorInfo)
        TempActorCreated(fromProtoActorInfo(data.getInfo1))

      case ProtoAnnotationType.TEMP_ACTOR_STOPPED ⇒
        // TempActorStopped(info: ActorInfo)
        TempActorStopped(fromProtoActorInfo(data.getInfo1))

      case ProtoAnnotationType.ACTOR_SELECTION_TOLD ⇒
        // ActorSelectionTold(info: ActorSelectionInfo, message: String, sender: Option[ActorInfo])
        val sender = if (data.hasInfo2) Some(fromProtoActorInfo(data.getInfo2)) else None
        ActorSelectionTold(fromProtoActorSelectionInfo(data.getInfo1), data.getString1, sender)

      case ProtoAnnotationType.ACTOR_SELECTION_ASKED ⇒
        // ActorSelectionAsked(info: ActorSelectionInfo, message: String)
        ActorSelectionAsked(fromProtoActorSelectionInfo(data.getInfo1), data.getString1)

      case ProtoAnnotationType.FUTURE_CREATED ⇒
        // FutureCreated(info: FutureInfo)
        FutureCreated(fromProtoFutureInfo(data.getInfo1))

      case ProtoAnnotationType.FUTURE_SCHEDULED ⇒
        // FutureScheduled(info: FutureInfo, taskInfo: TaskInfo)
        FutureScheduled(fromProtoFutureInfo(data.getInfo1), fromProtoTaskInfo(data.getInfo2))

      case ProtoAnnotationType.FUTURE_SUCCEEDED ⇒
        // FutureSucceeded(info: FutureInfo, result: String, resultInfo: Info)
        FutureSucceeded(fromProtoFutureInfo(data.getInfo1), data.getString1, fromProtoInfo(data.getInfo2))

      case ProtoAnnotationType.FUTURE_FAILED ⇒
        // FutureFailed(info: FutureInfo, exception: String)
        FutureFailed(fromProtoFutureInfo(data.getInfo1), data.getString1)

      case ProtoAnnotationType.FUTURE_CALLBACK_ADDED ⇒
        // FutureCallbackAdded(info: FutureInfo)
        FutureCallbackAdded(fromProtoFutureInfo(data.getInfo1))

      case ProtoAnnotationType.FUTURE_CALLBACK_STARTED ⇒
        // FutureCallbackStarted(info: FutureInfo)
        FutureCallbackStarted(fromProtoFutureInfo(data.getInfo1))

      case ProtoAnnotationType.FUTURE_CALLBACK_COMPLETED ⇒
        // FutureCallbackCompleted(info: FutureInfo)
        FutureCallbackCompleted(fromProtoFutureInfo(data.getInfo1))

      case ProtoAnnotationType.FUTURE_AWAITED ⇒
        // FutureAwaited(info: FutureInfo)
        FutureAwaited(fromProtoFutureInfo(data.getInfo1))

      case ProtoAnnotationType.FUTURE_AWAIT_TIMED_OUT ⇒
        // FutureAwaitTimedOut(info: FutureInfo, duration: String)
        FutureAwaitTimedOut(fromProtoFutureInfo(data.getInfo1), data.getString1)

      case ProtoAnnotationType.RUNNABLE_SCHEDULED ⇒
        // RunnableScheduled(info: TaskInfo)
        RunnableScheduled(fromProtoTaskInfo(data.getInfo1))

      case ProtoAnnotationType.RUNNABLE_STARTED ⇒
        // RunnableStarted(info: TaskInfo)
        RunnableStarted(fromProtoTaskInfo(data.getInfo1))

      case ProtoAnnotationType.RUNNABLE_COMPELETED ⇒
        // RunnableCompleted(info: TaskInfo)
        RunnableCompleted(fromProtoTaskInfo(data.getInfo1))

      case ProtoAnnotationType.SCHEDULED_ONCE ⇒
        // ScheduledOnce(info: TaskInfo, delay: String)
        ScheduledOnce(fromProtoTaskInfo(data.getInfo1), data.getString1)

      case ProtoAnnotationType.SCHEDULED_STARTED ⇒
        // ScheduledStarted(info: TaskInfo)
        ScheduledStarted(fromProtoTaskInfo(data.getInfo1))

      case ProtoAnnotationType.SCHEDULED_COMPLETED ⇒
        // ScheduledCompleted(info: TaskInfo)
        ScheduledCompleted(fromProtoTaskInfo(data.getInfo1))

      case ProtoAnnotationType.SCHEDULED_CANCELLED ⇒
        // ScheduledCancelled(info: TaskInfo)
        ScheduledCancelled(fromProtoTaskInfo(data.getInfo1))

      case ProtoAnnotationType.REMOTE_MESSAGE_SENT ⇒
        // RemoteMessageSent(info: ActorInfo, message: String, messageSize: Int)
        RemoteMessageSent(fromProtoActorInfo(data.getInfo1), data.getString1, data.getNumber.toInt)

      case ProtoAnnotationType.REMOTE_MESSAGE_RECEIVED ⇒
        // RemoteMessageReceived(info: ActorInfo, message: String, messageSize: Int)
        RemoteMessageReceived(fromProtoActorInfo(data.getInfo1), data.getString1, data.getNumber.toInt)

      case ProtoAnnotationType.REMOTE_MESSAGE_COMPLETED ⇒
        // RemoteMessageCompleted(info: ActorInfo, message: String)
        RemoteMessageCompleted(fromProtoActorInfo(data.getInfo1), data.getString1)

      case ProtoAnnotationType.REMOTE_STATUS ⇒
        // RemoteStatus(statusType: RemoteStatusType.Value, serverNode: Option[String], clientNode: Option[String], cause: Option[String])
        val statusType = RemoteStatusType.withName(data.getString1)
        val serverNode = if (data.hasString2) Some(data.getString2) else None
        val clientNode = if (data.hasString3) Some(data.getString3) else None
        val cause = if (data.hasString4) Some(data.getString4) else None
        RemoteStatus(statusType, serverNode, clientNode, cause)

      case ProtoAnnotationType.REMOTING_LIFECYCLE ⇒
        // RemotingLifecycle(eventType: RemotingLifecycleEventType.Value, localAddress: Option[String],
        //   remoteAddress: Option[String], inbound: Option[Boolean], cause: Option[String], listenAddresses: Set[String])
        val eventType = RemotingLifecycleEventType.withName(data.getString1)
        val localAddress = if (data.hasString2) Some(data.getString2) else None
        val remoteAddress = if (data.hasString3) Some(data.getString3) else None
        val inbound = if (data.hasBool1) Some(data.getBool1) else None
        val cause = if (data.hasString4) Some(data.getString4) else None
        val listenAddresses = data.getStringsList.asScala.toSet
        RemotingLifecycle(eventType, localAddress, remoteAddress, inbound, cause, listenAddresses)

      case ProtoAnnotationType.EVENT_STREAM_ERROR ⇒
        // EventStreamError(message: String)
        EventStreamError(data.getString1)

      case ProtoAnnotationType.EVENT_STREAM_WARNING ⇒
        // EventStreamWarning(message: String)
        EventStreamWarning(data.getString1)

      case ProtoAnnotationType.EVENT_STREAM_DEAD_LETTER ⇒
        // EventStreamDeadLetter(message: String, sender: ActorInfo, recipient: ActorInfo)
        EventStreamDeadLetter(data.getString1, fromProtoActorInfo(data.getInfo1), fromProtoActorInfo(data.getInfo2))

      case ProtoAnnotationType.EVENT_STREAM_UNHANDLED_MESSAGE ⇒
        // EventStreamUnhandledMessage(message: String, sender: ActorInfo, recipient: ActorInfo)
        EventStreamUnhandledMessage(data.getString1, fromProtoActorInfo(data.getInfo1), fromProtoActorInfo(data.getInfo2))

      case ProtoAnnotationType.DISPATCHER_STATUS ⇒
        // DispatcherStatus(dispatcher: String, dispatcherType: String, metrics: DispatcherMetrics)
        DispatcherStatus(data.getString1, data.getString2, fromProtoDispatcherMetrics(data.getDispatcherMetrics))

      case ProtoAnnotationType.SYSTEM_METRICS ⇒
        // see Annotation.scala
        fromProtoSystemMetrics(data.getSystemMetrics)

      case ProtoAnnotationType.DEADLOCKED_THREADS ⇒
        // DeadlockedThreads(threads: Set[String])
        fromProtoDeadlockedThreads(data.getDeadlockedThreads)

      case ProtoAnnotationType.ACTION_ROUTE_REQUEST ⇒
        val ri = fromProtoPlayActionRequestInfo(data.getPlayActionInfo.getRequestInfo)
        ActionRouteRequest(ri, data.getPlayActionInfo.getRouteRequestResult match {
          case ProtoActionRouteRequestResult.NO_HANDLER       ⇒ ActionRouteRequestResult.NoHandler
          case ProtoActionRouteRequestResult.ESSENTIAL_ACTION ⇒ ActionRouteRequestResult.EssentialAction
          case ProtoActionRouteRequestResult.WEB_SOCKET       ⇒ ActionRouteRequestResult.WebSocket
        })

      case ProtoAnnotationType.ACTION_ERROR ⇒
        val ri = fromProtoPlayActionRequestInfo(data.getPlayActionInfo.getRequestInfo)
        ActionError(ri, data.getString1, data.getPlayActionInfo.getStackTraceList.asScala.toSeq)

      case ProtoAnnotationType.ACTION_HANDLER_NOT_FOUND ⇒
        val ri = fromProtoPlayActionRequestInfo(data.getPlayActionInfo.getRequestInfo)
        ActionHandlerNotFound(ri)

      case ProtoAnnotationType.ACTION_BAD_REQUEST ⇒
        val ri = fromProtoPlayActionRequestInfo(data.getPlayActionInfo.getRequestInfo)
        ActionBadRequest(ri, data.getString1)

      case ProtoAnnotationType.ACTION_RESOLVED ⇒
        val extractor = fromProtoPlayActionResolvedInfo(data.getPlayActionInfo.getResolvedInfo)
        ActionResolved(ActionResolvedInfo(extractor.controller,
          extractor.method,
          extractor.parameterTypes,
          extractor.verb,
          extractor.comments,
          extractor.path))

      case ProtoAnnotationType.ACTION_INVOKED ⇒
        val extractor = fromProtoPlayActionInfo(data.getPlayActionInfo)
        ActionInvoked(ActionInvocationInfo(extractor.controller,
          extractor.method,
          extractor.pattern,
          extractor.id,
          extractor.uri,
          extractor.path,
          extractor.httpMethod,
          extractor.version,
          extractor.remoteAddress,
          extractor.host,
          extractor.domain,
          extractor.session))

      case ProtoAnnotationType.ACTION_RESULT_GENERATION_START ⇒
        ActionResultGenerationStart

      case ProtoAnnotationType.ACTION_RESULT_GENERATION_END ⇒
        ActionResultGenerationEnd

      case ProtoAnnotationType.ACTION_CHUNKED_INPUT_START ⇒
        ActionChunkedInputStart

      case ProtoAnnotationType.ACTION_CHUNKED_INPUT_END ⇒
        ActionChunkedInputEnd

      case ProtoAnnotationType.ACTION_CHUNKED_RESULT ⇒
        val extractor = fromProtoPlayActionInfo(data.getPlayActionInfo)
        ActionChunkedResult(ActionResultInfo(extractor.httpResponseCode))

      case ProtoAnnotationType.ACTION_SIMPLE_RESULT ⇒
        val extractor = fromProtoPlayActionInfo(data.getPlayActionInfo)
        ActionSimpleResult(ActionResultInfo(extractor.httpResponseCode))

      case ProtoAnnotationType.ACTION_ASYNC_RESULT ⇒
        ActionAsyncResult

      case ProtoAnnotationType.ITERATEE_CREATED ⇒
        val extractor = fromProtoPlayIterateeInfo(data.getPlayIterateeInfo)
        IterateeCreated(IterateeInfo(extractor.uuid))

      case ProtoAnnotationType.ITERATEE_FOLDED ⇒
        val extractor = fromProtoPlayIterateeInfo(data.getPlayIterateeInfo)
        IterateeFolded(IterateeInfo(extractor.uuid))

      case ProtoAnnotationType.ITERATEE_DONE ⇒
        val extractor = fromProtoPlayIterateeInfo(data.getPlayIterateeInfo)
        IterateeDone(IterateeInfo(extractor.uuid))

      case ProtoAnnotationType.ITERATEE_ERROR ⇒
        val extractor = fromProtoPlayIterateeInfo(data.getPlayIterateeInfo)
        IterateeError(IterateeInfo(extractor.uuid))

      case ProtoAnnotationType.ITERATEE_CONTINUED ⇒
        val extractor = fromProtoPlayIterateeInfo(data.getPlayIterateeInfo)
        IterateeContinued(IterateeInfo(extractor.uuid), extractor.input, IterateeInfo(extractor.nextUuid))

      case ProtoAnnotationType.NETTY_HTTP_RECEIVED_START ⇒
        NettyHttpReceivedStart

      case ProtoAnnotationType.NETTY_HTTP_RECEIVED_END ⇒
        NettyHttpReceivedEnd

      case ProtoAnnotationType.NETTY_PLAY_RECEIVED_START ⇒
        NettyPlayReceivedStart

      case ProtoAnnotationType.NETTY_PLAY_RECEIVED_END ⇒
        NettyPlayReceivedEnd

      case ProtoAnnotationType.NETTY_RESPONSE_HEADER ⇒
        val extractor = fromProtoPlayNettyInfo(data.getPlayNettyInfo)
        NettyResponseHeader(extractor.size)

      case ProtoAnnotationType.NETTY_RESPONSE_BODY ⇒
        val extractor = fromProtoPlayNettyInfo(data.getPlayNettyInfo)
        NettyResponseBody(extractor.size)

      case ProtoAnnotationType.NETTY_WRITE_CHUNK ⇒
        val extractor = fromProtoPlayNettyInfo(data.getPlayNettyInfo)
        NettyWriteChunk(extractor.overhead, extractor.size)

      case ProtoAnnotationType.NETTY_READ_BYTES ⇒
        val extractor = fromProtoPlayNettyInfo(data.getPlayNettyInfo)
        NettyReadBytes(extractor.size)
    }
  }

  private[util] def fromProtoSystemMessage(proto: ProtoAnnotationData): SysMsg = proto.getSysMsg match {
    case ProtoSystemMessageType.SYS_MSG_CREATE           ⇒ CreateSysMsg
    case ProtoSystemMessageType.SYS_MSG_SUSPEND          ⇒ SuspendSysMsg
    case ProtoSystemMessageType.SYS_MSG_RESUME           ⇒ ResumeSysMsg
    case ProtoSystemMessageType.SYS_MSG_TERMINATE        ⇒ TerminateSysMsg
    case ProtoSystemMessageType.SYS_MSG_RECREATE         ⇒ RecreateSysMsg(proto.getString1)
    case ProtoSystemMessageType.SYS_MSG_SUPERVISE        ⇒ SuperviseSysMsg(fromProtoActorInfo(proto.getInfo2))
    case ProtoSystemMessageType.SYS_MSG_CHILD_TERMINATED ⇒ ChildTerminatedSysMsg(fromProtoActorInfo(proto.getInfo2))
    case ProtoSystemMessageType.SYS_MSG_LINK             ⇒ LinkSysMsg(fromProtoActorInfo(proto.getInfo2))
    case ProtoSystemMessageType.SYS_MSG_UNLINK           ⇒ UnlinkSysMsg(fromProtoActorInfo(proto.getInfo2))
    case ProtoSystemMessageType.SYS_MSG_WATCH            ⇒ WatchSysMsg(fromProtoActorInfo(proto.getInfo2), fromProtoActorInfo(proto.getInfo3))
    case ProtoSystemMessageType.SYS_MSG_UNWATCH          ⇒ UnwatchSysMsg(fromProtoActorInfo(proto.getInfo2), fromProtoActorInfo(proto.getInfo3))
    case ProtoSystemMessageType.SYS_MSG_NO_MESSAGE       ⇒ NoMessageSysMsg
    case ProtoSystemMessageType.SYS_MSG_FAILED           ⇒ FailedSysMsg(fromProtoActorInfo(proto.getInfo2), proto.getString1)
    case ProtoSystemMessageType.SYS_MSG_DEATH_WATCH      ⇒ DeathWatchSysMsg(fromProtoActorInfo(proto.getInfo2), proto.getBool1, proto.getBool2)
  }

  private[util] def fromProtoInfo(info: ProtoInfo): Info = info.getType match {
    case ProtoInfoType.ACTOR_INFO    ⇒ fromProtoActorInfo(info)
    case ProtoInfoType.FUTURE_INFO   ⇒ fromProtoFutureInfo(info)
    case ProtoInfoType.TASK_INFO     ⇒ fromProtoTaskInfo(info)
    case ProtoInfoType.ITERATEE_INFO ⇒ fromProtoIterateeInfo(info)
    case _                           ⇒ NoInfo
  }

  private[util] def fromProtoActorInfo(info: ProtoInfo): ActorInfo = {
    // ActorInfo(path: String, dispatcher: Option[String], remote: Boolean, router: Boolean, tags: Set[String])
    val path = info.getPath
    val dispatcher = if (info.hasDispatcher) Some(info.getDispatcher) else None
    val remote = info.getRemote
    val router = info.getRouter
    val tags = info.getTagsList.asScala.toSet
    ActorInfo(path, dispatcher, remote, router, tags)
  }

  private[util] def fromProtoActorSelectionInfo(info: ProtoInfo): ActorSelectionInfo = {
    // ActorSelectionInfo(anchor: ActorInfo, path: String)
    val anchor = fromProtoActorInfo(info)
    val path = info.getPath2
    ActorSelectionInfo(anchor, path)
  }

  private[util] def fromProtoFutureInfo(info: ProtoInfo): FutureInfo = {
    // FutureInfo(uuid: UUID, dispatcher: String)
    val uuid = fromProtoUuid(info.getUuid)
    val dispatcher = info.getDispatcher
    FutureInfo(uuid, dispatcher)
  }

  private[util] def fromProtoTaskInfo(info: ProtoInfo): TaskInfo = {
    // TaskInfo(uuid: UUID, dispatcher: String)
    val uuid = fromProtoUuid(info.getUuid)
    val dispatcher = info.getDispatcher
    TaskInfo(uuid, dispatcher)
  }

  private[util] def fromProtoIterateeInfo(info: ProtoInfo): IterateeInfo = {
    // IterateeInfo(uuid: UUID)
    val uuid = fromProtoUuid(info.getUuid)
    IterateeInfo(uuid)
  }

  private[util] def fromProtoPlayActionResultInfo(info: ProtoPlayActionResultInfo): ActionResultInfo = {
    ActionResultInfo(info.getHttpResponseCode)
  }

  private[util] def fromProtoPlayActionResult(info: ProtoPlayActionResult): ActionResponseAnnotation = {
    val ri = fromProtoPlayActionResultInfo(info.getResultInfo)
    info.getType match {
      case ProtoAnnotationType.ACTION_SIMPLE_RESULT  ⇒ ActionSimpleResult(ri)
      case ProtoAnnotationType.ACTION_CHUNKED_RESULT ⇒ ActionChunkedResult(ri)
    }
  }

  private[util] def fromProtoDispatcherMetrics(metrics: ProtoDispatcherMetrics): DispatcherMetrics = {
    DispatcherMetrics(
      metrics.getCorePoolSize,
      metrics.getMaximumPoolSize,
      metrics.getKeepAliveTime,
      metrics.getRejectedHandler,
      metrics.getActiveThreadCount,
      metrics.getTaskCount,
      metrics.getCompletedTaskCount,
      metrics.getLargestPoolSize,
      metrics.getPoolSize,
      metrics.getQueueSize)
  }

  private[util] def fromProtoSystemMetrics(metrics: ProtoSystemMetrics): SystemMetrics = {
    val additional = if (metrics.hasAdditional) Some(fromProtoAdditionalSystemMetrics(metrics.getAdditional)) else None
    SystemMetrics(
      metrics.getRunningActors,
      metrics.getStartTime,
      metrics.getUpTime,
      metrics.getAvailableProcessors,
      metrics.getDaemonThreadCount,
      metrics.getThreadCount,
      metrics.getPeakThreadCount,
      metrics.getCommittedHeap,
      metrics.getMaxHeap,
      metrics.getUsedHeap,
      metrics.getCommittedNonHeap,
      metrics.getMaxNonHeap,
      metrics.getUsedNonHeap,
      metrics.getGcCountPerMinute,
      metrics.getGcTimePercent,
      metrics.getSystemLoadAverage,
      additional)
  }

  private[util] def fromProtoAdditionalSystemMetrics(metrics: ProtoAdditionalSystemMetrics): AdditionalSystemMetrics = {
    AdditionalSystemMetrics(
      CpuSystemMetrics(
        metrics.getCpuUser,
        metrics.getCpuSys,
        metrics.getCpuCombined,
        metrics.getPidCpu,
        metrics.getLoadAverage1Min,
        metrics.getLoadAverage5Min,
        metrics.getLoadAverage15Min,
        metrics.getContextSwitches),
      MemorySystemMetrics(
        metrics.getMemUsage,
        metrics.getMemSwapPageIn,
        metrics.getMemSwapPageOut),
      NetworkSystemMetrics(
        metrics.getTcpCurrEstab,
        metrics.getTcpEstabResets,
        metrics.getNetRxBytesRate,
        metrics.getNetTxBytesRate,
        metrics.getNetRxErrors,
        metrics.getNetTxErrors))
  }

  private[util] class ActionInvocationInfoExtractor(info: ProtoPlayActionInvocationInfo) {
    lazy val controller = info.getController
    lazy val method = info.getMethod
    lazy val pattern = info.getPattern
    lazy val id = info.getId
    lazy val uri = info.getUri
    lazy val path = info.getPath
    lazy val httpMethod = info.getHttpMethod
    lazy val version = info.getVersion
    lazy val remoteAddress = info.getRemoteAddress
    lazy val host = if (info.hasHost) Some(info.getHost) else None
    lazy val domain = if (info.hasDomain) Some(info.getDomain) else None
    lazy val session = if (info.getSessionCount > 0) Some(Map[String, String](info.getSessionList.asScala.map(fromProtoStringStringMapEntry).toSeq: _*)) else None
  }

  private[util] class ActionResolvedInfoExtractor(info: ProtoPlayActionResolvedInfo) {
    lazy val controller = info.getController
    lazy val method = info.getMethod
    lazy val path = info.getPath
    lazy val comments = info.getComments
    lazy val verb = info.getVerb
    lazy val parameterTypes = info.getParameterTypesList.asScala.toSeq
  }

  private[util] class ActionInfoExtractor(info: ProtoPlayActionInfo) extends ActionInvocationInfoExtractor(info.getInvocationInfo) {
    lazy val httpResponseCode = info.getResultInfo.getHttpResponseCode
  }

  private[util] class IterateeInfoExtractor(info: ProtoPlayIterateeInfo) {
    lazy val uuid = fromProtoUuid(info.getUuid)
    lazy val input = IterateeInput.fromInt(info.getTag, if (info.hasValue) Some(info.getValue) else None)
    lazy val nextUuid = fromProtoUuid(info.getNextUuid)
  }

  private[util] class NettyInfoExtractor(info: ProtoPlayNettyInfo) {
    val overhead = info.getOverhead
    val size = info.getSize
  }

  private[util] def fromProtoActionInvocationInfo(info: ProtoPlayActionInvocationInfo): ActionInvocationInfo = {
    val extractor = new ActionInvocationInfoExtractor(info)
    ActionInvocationInfo(extractor.controller,
      extractor.method,
      extractor.pattern,
      extractor.id,
      extractor.uri,
      extractor.path,
      extractor.httpMethod,
      extractor.version,
      extractor.remoteAddress,
      extractor.host,
      extractor.domain,
      extractor.session)
  }

  private[util] def fromProtoPlayIterateeInfo(info: ProtoPlayIterateeInfo): IterateeInfoExtractor = new IterateeInfoExtractor(info)

  private[util] def fromProtoPlayActionInfo(info: ProtoPlayActionInfo): ActionInfoExtractor = new ActionInfoExtractor(info)

  private[util] def fromProtoPlayActionResolvedInfo(info: ProtoPlayActionResolvedInfo): ActionResolvedInfoExtractor = new ActionResolvedInfoExtractor(info)

  private[util] def fromProtoStringStringMapEntry(data: ProtoStringStringMapEntry): (String, String) = (data.getKey, data.getValue)

  private[util] def fromProtoStringSeqStringMapEntry(data: ProtoStringSeqStringMapEntry): (String, Seq[String]) = (data.getKey, data.getValuesList.asScala.toSeq)

  private[util] def fromProtoPlayActionRequestInfo(info: ProtoPlayActionRequestInfo): ActionRequestInfo = {
    ActionRequestInfo(info.getId,
      Map[String, String](info.getTagsList.asScala.map(fromProtoStringStringMapEntry).toSeq: _*),
      info.getUri,
      info.getPath,
      info.getMethod,
      info.getVersion,
      Map[String, Seq[String]](info.getQueryStringList.asScala.map(fromProtoStringSeqStringMapEntry).toSeq: _*),
      Map[String, Seq[String]](info.getHeadersList.asScala.map(fromProtoStringSeqStringMapEntry).toSeq: _*))
  }

  private[util] def fromProtoPlayNettyInfo(info: ProtoPlayNettyInfo): NettyInfoExtractor = new NettyInfoExtractor(info)

  private[util] def fromProtoDeadlockedThreads(deadlocked: ProtoDeadlockedThreads): DeadlockedThreads = {
    DeadlockedThreads(deadlocked.getMessage, deadlocked.getDeadlocksList.asScala.toSet)
  }

  def fromProtoUuid(uuid: ProtoUuid): UUID = {
    new UUID(uuid.getHigh, uuid.getLow)
  }

  def fromProtoTraceContext(context: ProtoTraceContext): TraceContext = {
    if (context.hasTrace && context.hasParent && context.hasSampled)
      TraceContext(fromProtoUuid(context.getTrace), fromProtoUuid(context.getParent), context.getSampled)
    else
      TraceContext.ZeroTrace
  }
}
