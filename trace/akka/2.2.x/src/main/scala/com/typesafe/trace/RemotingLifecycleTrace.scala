/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */

package com.typesafe.trace

import akka.remote._
import com.typesafe.trace.RemotingLifecycleEventType._

/**
 * Tracing remoting lifecycle events.
 */
object RemotingLifecycleTrace {

  def remotingLifecycle(message: RemotingLifecycleEvent): RemotingLifecycle = message match {

    case AssociatedEvent(localAddress, remoteAddress, inbound) ⇒
      RemotingLifecycle(
        eventType = AssociatedEventType,
        localAddress = Some(localAddress.toString),
        remoteAddress = Some(remoteAddress.toString),
        inbound = Some(inbound))

    case DisassociatedEvent(localAddress, remoteAddress, inbound) ⇒
      RemotingLifecycle(
        eventType = DisassociatedEventType,
        localAddress = Some(localAddress.toString),
        remoteAddress = Some(remoteAddress.toString),
        inbound = Some(inbound))

    case AssociationErrorEvent(cause, localAddress, remoteAddress, inbound) ⇒
      RemotingLifecycle(
        eventType = AssociationErrorEventType,
        cause = Some(cause.toString),
        localAddress = Some(localAddress.toString),
        remoteAddress = Some(remoteAddress.toString),
        inbound = Some(inbound))

    case RemotingListenEvent(listenAddresses) ⇒
      RemotingLifecycle(
        eventType = RemotingListenEventType,
        listenAddresses = listenAddresses map (_.toString))

    case RemotingShutdownEvent ⇒
      RemotingLifecycle(eventType = RemotingShutdownEventType)

    case RemotingErrorEvent(cause) ⇒
      RemotingLifecycle(
        eventType = RemotingErrorEventType,
        cause = Some(cause.toString))

    case _ ⇒
      RemotingLifecycle(eventType = UnknownEventType)
  }
}
