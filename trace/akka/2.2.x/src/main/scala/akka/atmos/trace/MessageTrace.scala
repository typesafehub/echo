
package akka.atmos.trace

import akka.actor.AutoReceivedMessage
import akka.dispatch.sysmsg.SystemMessage
import com.typesafe.atmos.trace._

/**
 * Message tracing is separate as SystemMessage and AutoReceivedMessage are akka package private.
 */
class MessageTrace(trace: Trace) {

  val actorEvents = trace.settings.events.actors
  val systemMessageEvents = actorEvents && trace.settings.events.systemMessages
  val tempActorEvents = actorEvents && trace.settings.events.tempActors

  def tracingActors = actorEvents && trace.tracing
  def tracingSystemMessages = systemMessageEvents && trace.tracing
  def tracingTempActors = tempActorEvents && trace.tracing

  final def createSysMsg(message: SystemMessage): SysMsg = {
    import akka.dispatch.sysmsg._
    message match {
      case Create(_)                             ⇒ CreateSysMsg
      case Recreate(cause)                       ⇒ RecreateSysMsg(trace.formatShortException(cause))
      case Suspend()                             ⇒ SuspendSysMsg
      case Resume(_)                             ⇒ ResumeSysMsg
      case Terminate()                           ⇒ TerminateSysMsg
      case Supervise(child, _)                   ⇒ SuperviseSysMsg(TracedActorInfo(child))
      case Watch(watchee, watcher)               ⇒ WatchSysMsg(TracedActorInfo(watchee), TracedActorInfo(watcher))
      case Unwatch(watchee, watcher)             ⇒ UnwatchSysMsg(TracedActorInfo(watchee), TracedActorInfo(watcher))
      case NoMessage                             ⇒ NoMessageSysMsg
      case Failed(child, cause, _)               ⇒ FailedSysMsg(TracedActorInfo(child), trace.formatShortException(cause))
      case DeathWatchNotification(actor, ec, at) ⇒ DeathWatchSysMsg(TracedActorInfo(actor), ec, at)
    }
  }

  final def sysMsgDispatched(actor: ActorInfo, message: SystemMessage): TraceContext = {
    val sampled = trace.sampled
    if (sampled > 0)
      if (systemMessageEvents) trace.branch(SysMsgDispatched(actor, createSysMsg(message)), sampled)
      else trace.continue
    else if (trace.within && sampled == 0) TraceContext.NoTrace
    else TraceContext.EmptyTrace
  }

  final def sysMsgReceived(actor: ActorInfo, message: SystemMessage): Unit = {
    if (tracingSystemMessages) {
      trace.event(SysMsgReceived(actor, createSysMsg(message)))
    }
  }

  final def sysMsgCompleted(actor: ActorInfo, message: SystemMessage): Unit = {
    if (tracingSystemMessages) {
      trace.event(SysMsgCompleted(actor, createSysMsg(message)))
    }
  }

  def received(actor: ActorInfo, message: Any): Unit = {
    if (tracingActors) {
      message match {
        case msg: AutoReceivedMessage ⇒ trace.event(ActorAutoReceived(actor, trace.formatShortMessage(message)))
        case msg                      ⇒ trace.event(ActorReceived(actor, trace.formatShortMessage(message)))
      }
    }
  }

  def completed(actor: ActorInfo, message: Any): Unit = {
    if (tracingActors) {
      message match {
        case msg: AutoReceivedMessage ⇒ trace.event(ActorAutoCompleted(actor, trace.formatShortMessage(message)))
        case msg                      ⇒ trace.event(ActorCompleted(actor, trace.formatShortMessage(message)))
      }
    }
  }

  def tempReceived(actor: ActorInfo, message: Any): Unit = {
    if (tracingTempActors) {
      message match {
        case msg: AutoReceivedMessage ⇒ trace.event(ActorAutoReceived(actor, trace.formatShortMessage(message)))
        case msg                      ⇒ trace.event(ActorReceived(actor, trace.formatShortMessage(message)))
      }
    }
  }

  def tempCompleted(actor: ActorInfo, message: Any): Unit = {
    if (tracingTempActors) {
      message match {
        case msg: AutoReceivedMessage ⇒ trace.event(ActorAutoCompleted(actor, trace.formatShortMessage(message)))
        case msg                      ⇒ trace.event(ActorCompleted(actor, trace.formatShortMessage(message)))
      }
    }
  }
}
