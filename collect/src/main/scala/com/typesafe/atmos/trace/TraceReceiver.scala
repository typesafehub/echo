/**
 *  Copyright (C) 2011-2013 Typesafe <http://typesafe.com/>
 */
package com.typesafe.atmos.trace

import com.typesafe.atmos.trace.receive.ProtobufTraceReceiver
import com.typesafe.config.Config

object TraceReceiver {
  def apply(config: Config): TraceReceiver = {
    val eventHandler = TraceEventHandler(config)
    val port = config.getInt("atmos.trace.receive.port")
    val maxConnections = config.getInt("atmos.trace.receive.max-connections")
    new ProtobufTraceReceiver(eventHandler, port, maxConnections)
  }
}

trait TraceReceiver {
  def eventHandler(): TraceEventHandler
  def shutdown(): Unit
}
