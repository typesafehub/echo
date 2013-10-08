/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */

package com.typesafe.trace

import akka.actor._
import akka.routing.FromConfig
import com.typesafe.trace.test.EchoTraceSpec

object RoutedActorSpec {
  val config = """
    akka.actor.deployment {
      /pool {
        router = round-robin
        nr-of-instances = 2
      }
    }
    """

  class PoolActor extends Actor {
    def receive = {
      case message ⇒
    }
  }
}

class Akka22Scala211RoutedActorSpec extends EchoTraceSpec(RoutedActorSpec.config) {
  import RoutedActorSpec._

  "Tracing" must {
    "trace routed actors" in {
      val pool = system.actorOf(Props[PoolActor].withRouter(FromConfig()), "pool")
      eventCheck()

      pool ! "message"
      eventCheck()
    }
  }

}
