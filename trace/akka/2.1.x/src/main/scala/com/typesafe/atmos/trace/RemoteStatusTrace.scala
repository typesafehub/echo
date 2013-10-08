/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */

package com.typesafe.atmos.trace

import akka.remote._
import com.typesafe.atmos.trace.RemoteStatusType._

/**
 * Tracing remote life-cycle events.
 */
object RemoteStatusTrace {

  def remoteStatus(message: RemoteLifeCycleEvent): RemoteStatus = message match {
    case event: RemoteClientError ⇒
      RemoteStatus(
        statusType = RemoteClientErrorType,
        clientNode = Some(event.remoteAddress.toString),
        cause = Some(event.cause.toString))

    case event: RemoteClientDisconnected ⇒
      RemoteStatus(
        statusType = RemoteClientDisconnectedType,
        clientNode = Some(event.remoteAddress.toString))

    case event: RemoteClientConnected ⇒
      RemoteStatus(
        statusType = RemoteClientConnectedType,
        clientNode = Some(event.remoteAddress.toString))

    case event: RemoteClientStarted ⇒
      RemoteStatus(
        statusType = RemoteClientStartedType,
        clientNode = Some(event.remoteAddress.toString))

    case event: RemoteClientShutdown ⇒
      RemoteStatus(
        statusType = RemoteClientShutdownType,
        clientNode = Some(event.remoteAddress.toString))

    case event: RemoteServerStarted ⇒
      RemoteStatus(
        statusType = RemoteServerStartedType,
        serverNode = Some(event.remote.address.toString))

    case event: RemoteServerShutdown ⇒
      RemoteStatus(
        statusType = RemoteServerShutdownType,
        serverNode = Some(event.remote.address.toString))

    case event: RemoteServerError ⇒
      RemoteStatus(
        statusType = RemoteServerErrorType,
        serverNode = Some(event.remote.address.toString),
        cause = Some(event.cause.toString))

    case event: RemoteServerClientConnected ⇒
      RemoteStatus(
        statusType = RemoteServerClientConnectedType,
        clientNode = event.clientAddress.map(_.toString))

    case event: RemoteServerClientDisconnected ⇒
      RemoteStatus(
        statusType = RemoteServerClientDisconnectedType,
        clientNode = event.clientAddress.map(_.toString))

    case event: RemoteServerClientClosed ⇒
      RemoteStatus(
        statusType = RemoteServerClientClosedType,
        clientNode = event.clientAddress.map(_.toString))

    case _ ⇒ RemoteStatus(statusType = RemoteUnknownType)
  }
}
