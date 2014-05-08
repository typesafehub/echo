/**
 * Copyright (C) 2011-2013 Typesafe <http://typesafe.com/>
 */
package com.typesafe.trace.subscribe

import com.typesafe.trace.uuid.UUID

object SubscribeMessages {
  case class KeepAlive(targetInstance: String)
  case class Poll(targetInstance: String)
  case class Ack(timestamp: Long, uuid: UUID)
  case object EmptyAck

  // Test purpose
  case object SimulateException
}
