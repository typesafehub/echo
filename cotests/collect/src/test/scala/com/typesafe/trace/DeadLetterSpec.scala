/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */

package com.typesafe.trace

import com.typesafe.trace.test.AtmosCollectSpec

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

class Akka22Scala210DeadLetterSpec extends Akka22DeadLetterSpec
class Akka22Scala211DeadLetterSpec extends Akka22DeadLetterSpec

abstract class Akka22DeadLetterSpec extends DeadLetterSpec {
  // 10 events for system.actorOf
  // 4 events: ActorTold, ActorReceived, EventStreamDeadLetter, ActorCompleted
  // 9 events for poison pill
  val totalCount = 23
}

abstract class DeadLetterSpec extends AtmosCollectSpec {

  def totalCount: Int

  "Dead letter tracing" must {

    "record dead letter events" in {
      eventCheck(expected = totalCount) {
        countEventsOf[EventStreamDeadLetter] must be(1)
      }
    }

  }
}
