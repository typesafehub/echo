/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */

package com.typesafe.trace.util

import com.typesafe.trace.uuid.UUID

object Uuid {
  def apply() = new UUID()
  def zero() = UUID.nilUUID()
  def isZero(uuid: UUID) = { uuid.time == 0 && uuid.clockSeqAndNode == 0 }
}
