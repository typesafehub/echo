/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */

package com.typesafe.trace

import com.typesafe.trace.test.EchoCollectSpec

class Akka22RoutedActorSpec extends AkkaRoutedActorSpec

class Akka23Scala210RoutedActorSpec extends AkkaRoutedActorSpec
class Akka23Scala211RoutedActorSpec extends AkkaRoutedActorSpec

abstract class AkkaRoutedActorSpec extends EchoCollectSpec {

  "Tracing" must {
    "trace routed actors" in {
      eventCheck(expected = 32) {
        // creation of actors
      }

      eventCheck(expected = 4) {
        countTraces should be(1)

        countEventsOf[ActorTold] should be(2)
        countEventsOf[ActorReceived] should be(1)
        countEventsOf[ActorCompleted] should be(1)

        annotationsOf[ActorTold] foreach { a ⇒
          a.info.router should be(a.info.path endsWith "user/pool")
        }
      }
    }
  }

}
