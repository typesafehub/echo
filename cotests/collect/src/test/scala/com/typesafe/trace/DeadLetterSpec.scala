/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */

package com.typesafe.trace

import com.typesafe.trace.test.EchoCollectSpec

class Akka20DeadLetterSpec extends DeadLetterSpec {
  // 27 events for system.actorOf
  // 4 events: ActorTold, ActorReceived, EventStreamDeadLetter, ActorCompleted
  // 9 events for poison pill
  val totalCount = 40
}

class Akka21DeadLetterSpec extends DeadLetterSpec {
  // 10 events for system.actorOf
  // 4 events: ActorTold, ActorReceived, EventStreamDeadLetter, ActorCompleted
  // 9 events for poison pill
  val totalCount = 23
}

class Akka22DeadLetterSpec extends DeadLetterSpec {
  // 10 events for system.actorOf
  // 4 events: ActorTold, ActorReceived, EventStreamDeadLetter, ActorCompleted
  // 9 events for poison pill
  val totalCount = 23
}

class Akka23Scala210DeadLetterSpec extends Akka23DeadLetterSpec
class Akka23Scala211DeadLetterSpec extends Akka23DeadLetterSpec

abstract class Akka23DeadLetterSpec extends DeadLetterSpec {
  // 10 events for system.actorOf
  // 4 events: ActorTold, ActorReceived, EventStreamDeadLetter, ActorCompleted
  // 9 events for poison pill
  val totalCount = 23
}

abstract class DeadLetterSpec extends EchoCollectSpec {

  def totalCount: Int

  "Dead letter tracing" must {

    "record dead letter events" in {
      eventCheck(expected = totalCount) {
        countEventsOf[EventStreamDeadLetter] should be(1)
      }
    }

  }
}
