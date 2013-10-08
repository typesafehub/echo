/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */

package com.typesafe.trace

import akka.actor.{ DeadLetter, UnhandledMessage }
import akka.event.Logging.{ Error, Warning }

class EventStreamTrace(trace: Trace) {

  val tracingEventStream = trace.settings.events.eventStream

  def published(event: AnyRef): Unit =
    if (tracingEventStream) {
      event match {
        case Error(cause, logSource, logClass, message) ⇒
          trace.event(EventStreamError(trace.formatLongMessage(message)))
        case Warning(logSource, logClass, message) ⇒
          trace.event(EventStreamWarning(trace.formatLongMessage(message)))
        case DeadLetter(message, sender, recipient) ⇒
          val senderInfo = TracedActorInfo(sender)
          val recipientInfo = TracedActorInfo(recipient)
          if (recipientInfo ne TracedActorInfo.none)
            trace.event(EventStreamDeadLetter(trace.formatLongMessage(message), senderInfo, recipientInfo))
        case UnhandledMessage(message, sender, recipient) ⇒
          val senderInfo = TracedActorInfo(sender)
          val recipientInfo = TracedActorInfo(recipient)
          if (recipientInfo ne TracedActorInfo.none)
            trace.event(EventStreamUnhandledMessage(trace.formatLongMessage(message), senderInfo, recipientInfo))
        case _ ⇒
      }
    }
}
