/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */

package com.typesafe.atmos.trace

import akka.actor._
import com.typesafe.atmos.test.AtmosTraceSpec

object DeadLetterSpec {
  class Echo extends Actor {
    def receive = {
      case message ⇒ sender ! message
    }
  }
}

class Akka22Scala210DeadLetterSpec extends AtmosTraceSpec {
  import DeadLetterSpec._

  "Dead letter tracing" must {

    "record dead letter events" in {
      val echo = system.actorOf(Props[Echo], "echo")
      echo ! "something"
      echo ! PoisonPill
      eventCheck()
    }

  }
}
