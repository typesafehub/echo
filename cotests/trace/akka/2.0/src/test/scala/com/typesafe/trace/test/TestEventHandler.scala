package com.typesafe.trace.test

import akka.event.Logging._
import akka.event.slf4j.Slf4jEventHandler
import com.typesafe.trace.util.ExpectedFailureException

/**
 * Event handler that suppresses all errors with cause ExpectedFailureException
 * or errors and warnings with expected messages.
 */
class TestEventHandler extends Slf4jEventHandler {

  override def receive = suppress.orElse(super.receive)

  def suppress: Receive = {
    case event @ Error(cause: ExpectedFailureException, logSource, logClass, message) ⇒
      receive(Debug(logSource, logClass, message))
    case event @ Error(cause, logSource, logClass, message) if message.toString.startsWith("Expected error") ⇒
      receive(Debug(logSource, logClass, message))
    case event @ Warning(logSource, logClass, message) if message.toString.startsWith("Expected warning") ⇒
      receive(Debug(logSource, logClass, message))
  }
}
