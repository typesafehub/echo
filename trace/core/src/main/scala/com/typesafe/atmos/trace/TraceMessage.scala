/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */
package com.typesafe.atmos.trace

/**
 * Let your messages extend this trait to be able to define the message string
 * that are stored in the trace annotations.
 */
trait TraceMessage {

  /**
   * String representation of the message.
   */
  def toLongTraceMessage(): String

  /**
   * String representation of the message used in other than ActorTold
   * such as ActorReceived and ActorCompleted.
   */
  def toShortTraceMessage(): String

}
