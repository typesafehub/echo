/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */

package com.typesafe.trace

import akka.actor._
import com.typesafe.trace.test.AtmosTraceSpec

object UnhandledMessageSpec {
  case object Msg
  case object Other

  class MsgOnly extends Actor {
    def receive = {
      case Msg ⇒
    }
  }
}

class Akka20UnhandledMessageSpec extends AtmosTraceSpec {
  import UnhandledMessageSpec._

  "Unhandled message tracing" must {

    "record unhandled message events" in {
      val msgOnly = system.actorOf(Props[MsgOnly], "msgOnly")
      msgOnly ! Other
      msgOnly ! PoisonPill

      eventCheck()
    }

  }
}
