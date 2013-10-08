/**
 *  Copyright (C) 2009-2012 Typesafe Inc. <http://www.typesafe.com>
 */

package com.typesafe.trace

import akka.actor._
import akka.dispatch.Await
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.trace.test.AtmosTraceSpec
import com.typesafe.trace.uuid.UUID

class Akka20RemoteTraceSpec extends AtmosTraceSpec(RemoteTraceTest.config2) {
  import RemoteTraceTest._

  override def cotestNodes = 3
  override def cotestName = "trace2"

  "Remote tracing" must {

    "trace across nodes" in {
      barrier("setup")

      val actorB = system.actorOf(Props[ActorB], "b")

      barrier("start")

      barrier("check-trace")

      implicit val timeout = Timeout(timeoutHandler.time, timeoutHandler.unit)
      val traces = Await.result(actorB ? GetTraces, timeout.duration).asInstanceOf[Set[UUID]]

      traces.size must be(1)

      eventCheck("1")

      barrier("stop")

      eventCheck("2")
    }
  }
}
