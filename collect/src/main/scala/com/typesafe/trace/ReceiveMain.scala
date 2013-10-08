/**
 *  Copyright (C) 2011-2013 Typesafe <http://typesafe.com/>
 */
package com.typesafe.trace

import com.typesafe.config.{ Config, ConfigFactory }

/**
 * For running a singleton trace receive daemon.
 */
object ReceiveMain {
  private var receiver: Option[TraceReceiver] = None

  def startReceiver(config: Config): Unit = synchronized {
    val traceReceiver = TraceReceiver(config)
    receiver = Some(traceReceiver)
  }

  def shutdownReceiver(): Unit = synchronized {
    receiver foreach { _.shutdown() }
  }

  def getReceiver(): Option[TraceReceiver] = synchronized { receiver }

  def main(args: Array[String]) {
    val config = ConfigFactory.load()
    config.checkValid(ConfigFactory.defaultReference, "atmos")
    startReceiver(config)
    Runtime.getRuntime.addShutdownHook(new Thread {
      override def run = shutdownReceiver()
    })
  }
}
