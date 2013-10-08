/**
 *  Copyright (C) 2011-2013 Typesafe <http://typesafe.com/>
 */
package com.typesafe.atmos.trace

import com.typesafe.atmos.trace.send.{ ProtobufTraceSender }

object TraceSender {
  def apply(port: Int, capacity: Int, retry: Boolean, daemonic: Boolean, warn: Boolean): TraceSender = {
    try {
      new ProtobufTraceSender(port, capacity, retry, daemonic, warn)
    } catch {
      case e: Exception ⇒ DisabledTraceSender
    }
  }
}

trait TraceSender {
  def send(batch: Batch): Unit
  def shutdown(): Unit
}

object DisabledTraceSender extends TraceSender {
  def send(batch: Batch): Unit = ()
  def shutdown(): Unit = ()
}
