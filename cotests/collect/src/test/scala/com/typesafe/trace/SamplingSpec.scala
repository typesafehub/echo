/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */

package com.typesafe.trace

import com.typesafe.trace.test.EchoCollectSpec
import scala.concurrent.duration._

class Akka20SamplingSpec extends AkkaSamplingSpec {
  val totalCount = 84
  val sampled1Count = 66
}

class Akka21SamplingSpec extends AkkaSamplingSpec {
  val totalCount = 50
  val sampled1Count = 32
}

class Akka22Scala210SamplingSpec extends Akka22SamplingSpec
class Akka22Scala211SamplingSpec extends Akka22SamplingSpec

abstract class Akka22SamplingSpec extends AkkaSamplingSpec {
  val totalCount = 50
  val sampled1Count = 32
}

abstract class AkkaSamplingSpec extends EchoCollectSpec {

  def totalCount: Int
  def sampled1Count: Int

  "Sampling" must {

    "only trace at sampling rate" in {
      eventCheck(expected = totalCount, delay = 1.second) {
        val expectedTraces = 7
        val expectedSampled1 = sampled1Count
        val expectedSampled3 = 18
        countTraces must be(expectedTraces)
        val sampled = allEvents.groupBy(_.sampled)
        sampled.getOrElse(1, Nil).size must be(expectedSampled1)
        sampled.getOrElse(3, Nil).size must be(expectedSampled3)
      }
    }

  }
}
