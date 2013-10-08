/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */

package com.typesafe.trace

import com.typesafe.trace.test.AtmosCollectSpec

class Akka22Scala210RoutedActorSpec extends Akka22RoutedActorSpec
class Akka22Scala211RoutedActorSpec extends Akka22RoutedActorSpec

abstract class Akka22RoutedActorSpec extends AtmosCollectSpec {

  "Tracing" must {
    "trace routed actors" in {
      eventCheck(expected = 32) {
        // creation of actors
      }

      eventCheck(expected = 4) {
        countTraces must be(1)

        countEventsOf[ActorTold] must be(2)
        countEventsOf[ActorReceived] must be(1)
        countEventsOf[ActorCompleted] must be(1)

        annotationsOf[ActorTold] foreach { a ⇒
          a.info.router must be(a.info.path endsWith "user/pool")
        }
      }
    }
  }

}
