/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */

package com.typesafe.atmos.trace

import com.typesafe.atmos.util.Uuid
import com.typesafe.atmos.uuid.UUID

// -----------------------------------------------
// Annotation
// -----------------------------------------------

sealed trait Annotation

// -----------------------------------------------
// Info
// -----------------------------------------------

sealed trait Info

case object NoInfo extends Info

/**
 * Interface for getting an attached Info from Scala.
 */
trait TraceInfo {
  def info: Info
}

object TraceInfo {
  /**
   * Get the attached info for some traced object, if it exists.
   */
  def apply(traced: Any): Info = traced match {
    case attached: TraceInfo ⇒
      val info = attached.info
      if (info eq null) NoInfo else info
    case _ ⇒ NoInfo
  }
}

// ---------------------------------------------------------------------------
// Group annotations, for grouping together events as a single trace tree node
// ---------------------------------------------------------------------------

sealed trait GroupAnnotation extends Annotation {
  def name: String
}

case class GroupStarted(name: String) extends GroupAnnotation
case class GroupEnded(name: String) extends GroupAnnotation

// -----------------------------------------------
// User defined marker annotations
// -----------------------------------------------

sealed trait MarkerAnnotation extends Annotation {
  def name: String
}

case class MarkerStarted(name: String) extends MarkerAnnotation
case class Marker(name: String, data: String) extends MarkerAnnotation
case class MarkerEnded(name: String) extends MarkerAnnotation

// -----------------------------------------------
// Actor info
// -----------------------------------------------

case class ActorInfo(path: String, dispatcher: Option[String], remote: Boolean, router: Boolean, tags: Set[String]) extends Info

// -----------------------------------------------
// System annotations
// -----------------------------------------------

sealed trait SystemAnnotation extends Annotation

case class SystemStarted(startTime: Long) extends SystemAnnotation
case class SystemShutdown(shutdownTime: Long) extends SystemAnnotation

case class TopLevelActorRequested(guardian: ActorInfo, name: String) extends SystemAnnotation
case class TopLevelActorCreated(info: ActorInfo) extends SystemAnnotation

// -----------------------------------------------
// System message annotations
// -----------------------------------------------

sealed trait SysMsg

case object CreateSysMsg extends SysMsg
case object SuspendSysMsg extends SysMsg
case object ResumeSysMsg extends SysMsg
case object TerminateSysMsg extends SysMsg

case class RecreateSysMsg(cause: String) extends SysMsg
case class SuperviseSysMsg(child: ActorInfo) extends SysMsg
case class ChildTerminatedSysMsg(child: ActorInfo) extends SysMsg

// Akka 2.0
case class LinkSysMsg(subject: ActorInfo) extends SysMsg
case class UnlinkSysMsg(subject: ActorInfo) extends SysMsg

// Akka 2.1
case class WatchSysMsg(watchee: ActorInfo, watcher: ActorInfo) extends SysMsg
case class UnwatchSysMsg(watchee: ActorInfo, watcher: ActorInfo) extends SysMsg

// Akka 2.2
case class FailedSysMsg(child: ActorInfo, cause: String) extends SysMsg
case class DeathWatchSysMsg(actor: ActorInfo, existenceConfirmed: Boolean, addressTerminated: Boolean) extends SysMsg

case object NoMessageSysMsg extends SysMsg

sealed trait SysMsgAnnotation extends Annotation {
  def info: ActorInfo
  def message: SysMsg
}

case class SysMsgDispatched(info: ActorInfo, message: SysMsg) extends SysMsgAnnotation
case class SysMsgReceived(info: ActorInfo, message: SysMsg) extends SysMsgAnnotation
case class SysMsgCompleted(info: ActorInfo, message: SysMsg) extends SysMsgAnnotation

// -----------------------------------------------
// Actor annotations
// -----------------------------------------------

sealed trait ActorAnnotation extends Annotation {
  def info: ActorInfo
}

case class ActorRequested(info: ActorInfo, actor: ActorInfo) extends ActorAnnotation
case class ActorCreated(info: ActorInfo) extends ActorAnnotation
case class ActorTold(info: ActorInfo, message: String, sender: Option[ActorInfo]) extends ActorAnnotation
case class ActorAutoReceived(info: ActorInfo, message: String) extends ActorAnnotation
case class ActorAutoCompleted(info: ActorInfo, message: String) extends ActorAnnotation
case class ActorReceived(info: ActorInfo, message: String) extends ActorAnnotation
case class ActorCompleted(info: ActorInfo, message: String) extends ActorAnnotation
case class ActorAsked(info: ActorInfo, message: String) extends ActorAnnotation
case class ActorFailed(info: ActorInfo, reason: String, supervisor: ActorInfo) extends ActorAnnotation
case class TempActorCreated(info: ActorInfo) extends ActorAnnotation
case class TempActorStopped(info: ActorInfo) extends ActorAnnotation

// -----------------------------------------------
// Actor selection annotations
// -----------------------------------------------

case class ActorSelectionInfo(anchor: ActorInfo, path: String) extends Info

sealed trait ActorSelectionAnnotation extends Annotation with TraceInfo {
  def info: ActorSelectionInfo
}

case class ActorSelectionTold(info: ActorSelectionInfo, message: String, sender: Option[ActorInfo]) extends ActorSelectionAnnotation
case class ActorSelectionAsked(info: ActorSelectionInfo, message: String) extends ActorSelectionAnnotation

// -----------------------------------------------
// Future annotations
// -----------------------------------------------

case class FutureInfo(uuid: UUID = Uuid(), dispatcher: String = "") extends Info

sealed trait FutureAnnotation extends Annotation with TraceInfo {
  def info: FutureInfo
}

case class FutureCreated(info: FutureInfo) extends FutureAnnotation
case class FutureScheduled(info: FutureInfo, taskInfo: TaskInfo) extends FutureAnnotation
case class FutureSucceeded(info: FutureInfo, result: String, resultInfo: Info) extends FutureAnnotation
case class FutureFailed(info: FutureInfo, exception: String) extends FutureAnnotation
case class FutureCallbackAdded(info: FutureInfo) extends FutureAnnotation
case class FutureCallbackStarted(info: FutureInfo) extends FutureAnnotation
case class FutureCallbackCompleted(info: FutureInfo) extends FutureAnnotation
case class FutureAwaited(info: FutureInfo) extends FutureAnnotation
case class FutureAwaitTimedOut(info: FutureInfo, duration: String) extends FutureAnnotation

// -----------------------------------------------
// Action annotations
// -----------------------------------------------

case class ActionResolvedInfo(controller: String,
                              method: String,
                              parameterTypes: Seq[String],
                              verb: String,
                              comments: String,
                              path: String)

case class ActionInvocationInfo(controller: String,
                                method: String,
                                pattern: String,
                                id: Long,
                                uri: String,
                                path: String,
                                httpMethod: String,
                                version: String,
                                remoteAddress: String,
                                host: Option[String] = None,
                                domain: Option[String] = None,
                                session: Option[Map[String, String]] = None)

case class ActionResultInfo(httpResponseCode: Int)

case class ActionRequestInfo(id: Long,
                             tags: Map[String, String],
                             uri: String,
                             path: String,
                             method: String,
                             version: String,
                             queryString: Map[String, Seq[String]],
                             headers: Map[String, Seq[String]])

sealed trait ActionAnnotation extends Annotation

sealed trait ActionErrorAnnotation extends ActionAnnotation {
  def requestInfo: ActionRequestInfo
}

sealed trait ActionResponseAnnotation extends ActionAnnotation {
  def resultInfo: ActionResultInfo
}

sealed trait ActionRouteRequestResult {
  def toString: String
}
object ActionRouteRequestResult {
  def fromStringOption(in: String): Option[ActionRouteRequestResult] = in.trim.toLowerCase match {
    case "nohandler"       ⇒ Some(NoHandler)
    case "essentialaction" ⇒ Some(EssentialAction)
    case "websocket"       ⇒ Some(WebSocket)
    case _                 ⇒ None
  }
  def fromString(in: String): ActionRouteRequestResult = fromStringOption(in).get
  case object NoHandler extends ActionRouteRequestResult {
    override def toString = "NoHandler"
  }
  case object EssentialAction extends ActionRouteRequestResult {
    override def toString = "EssentialAction"
  }
  case object WebSocket extends ActionRouteRequestResult {
    override def toString = "WebSocket"
  }
}

case class ActionResolved(resolutionInfo: ActionResolvedInfo) extends ActionAnnotation
case class ActionInvoked(invocationInfo: ActionInvocationInfo) extends ActionAnnotation
case object ActionResultGenerationStart extends ActionAnnotation
case object ActionResultGenerationEnd extends ActionAnnotation
case object ActionChunkedInputStart extends ActionAnnotation
case object ActionChunkedInputEnd extends ActionAnnotation
case class ActionChunkedResult(resultInfo: ActionResultInfo) extends ActionResponseAnnotation
case class ActionSimpleResult(resultInfo: ActionResultInfo) extends ActionResponseAnnotation
case object ActionAsyncResult extends ActionAnnotation
case class ActionRouteRequest(requestInfo: ActionRequestInfo, result: ActionRouteRequestResult) extends ActionAnnotation
case class ActionError(requestInfo: ActionRequestInfo, message: String, stackTrace: Seq[String]) extends ActionErrorAnnotation
case class ActionHandlerNotFound(requestInfo: ActionRequestInfo) extends ActionErrorAnnotation
case class ActionBadRequest(requestInfo: ActionRequestInfo, error: String) extends ActionErrorAnnotation

// -----------------------------------------------
// Iteratee annotations
// -----------------------------------------------

case class IterateeInfo(uuid: UUID = Uuid()) extends Info

sealed trait IterateeAnnotation extends Annotation with TraceInfo {
  def info: IterateeInfo
}

object IterateeInput {
  final val ElTag = 1
  final val EmptyTag = 2
  final val EOFTag = 3

  sealed class Tag(val tag: Int)
  case class El(value: String) extends Tag(ElTag)
  case object Empty extends Tag(EmptyTag)
  case object EOF extends Tag(EOFTag)

  def fromInt(i: Int, value: Option[String] = None): Tag = i match {
    case ElTag    ⇒ El(value.get)
    case EmptyTag ⇒ Empty
    case EOFTag   ⇒ EOF
  }
}

case class IterateeCreated(info: IterateeInfo) extends IterateeAnnotation
case class IterateeFolded(info: IterateeInfo) extends IterateeAnnotation
case class IterateeDone(info: IterateeInfo) extends IterateeAnnotation
case class IterateeContinued(info: IterateeInfo, input: IterateeInput.Tag, nextInfo: IterateeInfo) extends IterateeAnnotation
case class IterateeError(info: IterateeInfo) extends IterateeAnnotation

// -----------------------------------------------
// Netty annotations
// -----------------------------------------------

sealed trait NettyAnnotation extends Annotation

case object NettyHttpReceivedStart extends NettyAnnotation
case object NettyHttpReceivedEnd extends NettyAnnotation
case object NettyPlayReceivedStart extends NettyAnnotation
case object NettyPlayReceivedEnd extends NettyAnnotation
case class NettyResponseHeader(size: Int) extends NettyAnnotation
case class NettyResponseBody(size: Int) extends NettyAnnotation
case class NettyWriteChunk(overhead: Int, size: Int) extends NettyAnnotation
case class NettyReadBytes(size: Int) extends NettyAnnotation

// -----------------------------------------------
// Runnable annotations
// -----------------------------------------------

case class TaskInfo(uuid: UUID = Uuid(), dispatcher: String = "") extends Info

sealed trait RunnableAnnotation extends Annotation {
  def info: TaskInfo
}

case class RunnableScheduled(info: TaskInfo) extends RunnableAnnotation
case class RunnableStarted(info: TaskInfo) extends RunnableAnnotation
case class RunnableCompleted(info: TaskInfo) extends RunnableAnnotation

// -----------------------------------------------
// Scheduler annotations
// -----------------------------------------------

sealed trait ScheduledAnnotation extends Annotation {
  def info: TaskInfo
}

case class ScheduledOnce(info: TaskInfo, delay: String) extends ScheduledAnnotation
case class ScheduledStarted(info: TaskInfo) extends ScheduledAnnotation
case class ScheduledCompleted(info: TaskInfo) extends ScheduledAnnotation
case class ScheduledCancelled(info: TaskInfo) extends ScheduledAnnotation

// -----------------------------------------------
// Remote message annotations
// -----------------------------------------------

sealed trait RemoteMessageAnnotation extends Annotation {
  def info: ActorInfo
  def message: String
}

case class RemoteMessageSent(info: ActorInfo, message: String, messageSize: Int) extends RemoteMessageAnnotation
case class RemoteMessageReceived(info: ActorInfo, message: String, messageSize: Int) extends RemoteMessageAnnotation
case class RemoteMessageCompleted(info: ActorInfo, message: String) extends RemoteMessageAnnotation

// -----------------------------------------------
// Remote status annotations
// -----------------------------------------------

case class RemoteStatus(
  statusType: RemoteStatusType.Value,
  serverNode: Option[String] = None,
  clientNode: Option[String] = None,
  cause: Option[String] = None) extends Annotation

object RemoteStatusType extends Enumeration {
  type RemoteStatusType = Value
  val RemoteClientErrorType = Value("remoteClientError")
  val RemoteClientDisconnectedType = Value("remoteClientDisconnected")
  val RemoteClientConnectedType = Value("remoteClientConnected")
  val RemoteClientStartedType = Value("remoteClientStarted")
  val RemoteClientShutdownType = Value("remoteClientShutdown")
  val RemoteClientWriteFailedType = Value("remoteClientWriteFailed")
  val RemoteServerStartedType = Value("remoteServerStarted")
  val RemoteServerShutdownType = Value("remoteServerShutdown")
  val RemoteServerErrorType = Value("remoteServerError")
  val RemoteServerClientConnectedType = Value("remoteServerClientConnected")
  val RemoteServerClientDisconnectedType = Value("remoteServerClientDisconnected")
  val RemoteServerClientClosedType = Value("remoteServerClientClosed")
  val RemoteUnknownType = Value("remoteUnknown")
}

// -----------------------------------------------
// Akka 2.2 remoting lifecycle annotations
// -----------------------------------------------

case class RemotingLifecycle(
  eventType: RemotingLifecycleEventType.Value,
  localAddress: Option[String] = None,
  remoteAddress: Option[String] = None,
  inbound: Option[Boolean] = None,
  cause: Option[String] = None,
  listenAddresses: Set[String] = Set.empty) extends Annotation

object RemotingLifecycleEventType extends Enumeration {
  type RemotingLifecycleEventType = Value
  val AssociatedEventType = Value("associated")
  val DisassociatedEventType = Value("disassociated")
  val AssociationErrorEventType = Value("associationError")
  val RemotingListenEventType = Value("remotingListen")
  val RemotingShutdownEventType = Value("remotingShutdown")
  val RemotingErrorEventType = Value("remotingError")
  val UnknownEventType = Value("unknown")
}

// -----------------------------------------------
// Event steam annotations
// -----------------------------------------------

sealed trait EventStreamAnnotation extends Annotation {
  def message: String
}

case class EventStreamError(message: String) extends EventStreamAnnotation
case class EventStreamWarning(message: String) extends EventStreamAnnotation
case class EventStreamDeadLetter(message: String, sender: ActorInfo, recipient: ActorInfo) extends EventStreamAnnotation
case class EventStreamUnhandledMessage(message: String, sender: ActorInfo, recipient: ActorInfo) extends EventStreamAnnotation

// -----------------------------------------------
// Metrics
// -----------------------------------------------

case class DispatcherStatus(
  dispatcher: String,
  dispatcherType: String,
  metrics: DispatcherMetrics) extends Annotation

case class DispatcherMetrics(
  corePoolSize: Int,
  maximumPoolSize: Int,
  keepAliveTime: Long,
  rejectedHandler: String,
  activeThreadCount: Int,
  taskCount: Long,
  completedTaskCount: Long,
  largestPoolSize: Long,
  poolSize: Long,
  queueSize: Long)

case class SystemMetrics(
  runningActors: Long,
  startTime: Long,
  upTime: Long,
  availableProcessors: Int,
  daemonThreadCount: Int,
  threadCount: Int,
  peakThreadCount: Int,
  committedHeap: Long,
  maxHeap: Long,
  usedHeap: Long,
  committedNonHeap: Long,
  maxNonHeap: Long,
  usedNonHeap: Long,
  gcCountPerMinute: Double,
  gcTimePercent: Double,
  systemLoadAverage: Double,
  additional: Option[AdditionalSystemMetrics] = None) extends Annotation

case class DeadlockedThreads(message: String, deadlocks: Set[String]) extends Annotation

case class AdditionalSystemMetrics(
  cpu: CpuSystemMetrics,
  memory: MemorySystemMetrics,
  network: NetworkSystemMetrics)

case class CpuSystemMetrics(
  cpuUser: Double,
  cpuSys: Double,
  cpuCombined: Double,
  pidCpu: Double,
  loadAverage1min: Double,
  loadAverage5min: Double,
  loadAverage15min: Double,
  contextSwitches: Long)

case class MemorySystemMetrics(
  memUsage: Double,
  memSwapPageIn: Long,
  memSwapPageOut: Long)

case class NetworkSystemMetrics(
  tcpCurrEstab: Long,
  tcpEstabResets: Long,
  netRxBytesRate: Long,
  netTxBytesRate: Long,
  netRxErrors: Long,
  netTxErrors: Long)
