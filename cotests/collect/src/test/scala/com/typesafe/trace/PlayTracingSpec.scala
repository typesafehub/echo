/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */

package com.typesafe.trace

import com.typesafe.trace.test.EchoCollectSpec
import scala.concurrent.duration._

class Play21NettyGetTracingSpec extends EchoCollectSpec {
  "Play Netty (GETs only)" must {
    "GET /get" in {
      eventCheck(expected = 121) {
        // allEvents.toSeq.sortBy(_.nanoTime).foreach(println)
        // printTraces
        countTraces must be(1)
        countEventsOf[NettyHttpReceivedStart.type] must be(1)
        countEventsOf[NettyHttpReceivedEnd.type] must be(1)
        countEventsOf[NettyPlayReceivedStart.type] must be(1)
        countEventsOf[NettyPlayReceivedEnd.type] must be(1)
        countEventsOf[ActionResolved] must be(1)
        countEventsOf[ActionRouteRequest] must be(1)
        countEventsOf[ActionInvoked] must be(1)
        countEventsOf[ActionSimpleResult] must be(1)
        countEventsOf[NettyResponseHeader] must be(1)
        countEventsOf[NettyResponseBody] must be(1)
        countEventsOf[NettyReadBytes] must be(1)
        countEventsOf[ActionResultGenerationStart.type] must be(1)
        countEventsOf[ActionResultGenerationEnd.type] must be(1)
      }
    }
    "GET /getWithSession" in {
      eventCheck(expected = 242) {
        // printTraces
        countTraces must be(2)
        countEventsOf[NettyHttpReceivedStart.type] must be(2)
        countEventsOf[NettyHttpReceivedEnd.type] must be(2)
        countEventsOf[NettyPlayReceivedStart.type] must be(2)
        countEventsOf[NettyPlayReceivedEnd.type] must be(2)
        countEventsOf[ActionResolved] must be(2)
        countEventsOf[ActionRouteRequest] must be(2)
        countEventsOf[ActionInvoked] must be(2)
        countEventsOf[ActionSimpleResult] must be(2)
        countEventsOf[NettyResponseHeader] must be(2)
        countEventsOf[NettyResponseBody] must be(2)
        countEventsOf[NettyReadBytes] must be(2)
        countEventsOf[ActionResultGenerationStart.type] must be(2)
        countEventsOf[ActionResultGenerationEnd.type] must be(2)
        eventsOf[ActionInvoked].exists(_.annotation.asInstanceOf[ActionInvoked].invocationInfo.session == Some(Map("key" -> "value"))) must be(true)
      }
    }
    "GET /get/sync/10" in {
      eventCheck(expected = 121) {
        // printTraces
        countTraces must be(1)
        countEventsOf[NettyHttpReceivedStart.type] must be(1)
        countEventsOf[NettyHttpReceivedEnd.type] must be(1)
        countEventsOf[NettyPlayReceivedStart.type] must be(1)
        countEventsOf[NettyPlayReceivedEnd.type] must be(1)
        countEventsOf[ActionResolved] must be(1)
        countEventsOf[ActionRouteRequest] must be(1)
        countEventsOf[ActionInvoked] must be(1)
        countEventsOf[ActionSimpleResult] must be(1)
        countEventsOf[NettyResponseHeader] must be(1)
        countEventsOf[NettyResponseBody] must be(1)
        countEventsOf[NettyReadBytes] must be(1)
        countEventsOf[ActionResultGenerationStart.type] must be(1)
        countEventsOf[ActionResultGenerationEnd.type] must be(1)
      }
    }
    "GET /get/async/100 -- 100ms delay" in {
      eventCheck(expected = 122) {
        // printTraces
        countTraces must be(1)
        countEventsOf[NettyHttpReceivedStart.type] must be(1)
        countEventsOf[NettyHttpReceivedEnd.type] must be(1)
        countEventsOf[NettyPlayReceivedStart.type] must be(1)
        countEventsOf[NettyPlayReceivedEnd.type] must be(1)
        countEventsOf[ActionResolved] must be(1)
        countEventsOf[ActionRouteRequest] must be(1)
        countEventsOf[ActionInvoked] must be(1)
        countEventsOf[ActionSimpleResult] must be(1)
        countEventsOf[ActionAsyncResult.type] must be(1)
        countEventsOf[NettyResponseHeader] must be(1)
        countEventsOf[NettyResponseBody] must be(1)
        countEventsOf[NettyReadBytes] must be(1)
        countEventsOf[ActionResultGenerationStart.type] must be(1)
        countEventsOf[ActionResultGenerationEnd.type] must be(1)
      }
    }
    "GET /get/async_immediate -- no delay" in {
      eventCheck(expected = 122) {
        // printTraces
        countTraces must be(1)
        countEventsOf[NettyHttpReceivedStart.type] must be(1)
        countEventsOf[NettyHttpReceivedEnd.type] must be(1)
        countEventsOf[NettyPlayReceivedStart.type] must be(1)
        countEventsOf[NettyPlayReceivedEnd.type] must be(1)
        countEventsOf[ActionResolved] must be(1)
        countEventsOf[ActionRouteRequest] must be(1)
        countEventsOf[ActionInvoked] must be(1)
        countEventsOf[ActionSimpleResult] must be(1)
        countEventsOf[ActionAsyncResult.type] must be(1)
        countEventsOf[NettyResponseHeader] must be(1)
        countEventsOf[NettyResponseBody] must be(1)
        countEventsOf[NettyReadBytes] must be(1)
        countEventsOf[ActionResultGenerationStart.type] must be(1)
        countEventsOf[ActionResultGenerationEnd.type] must be(1)
      }
    }
    "GET /get/chunked/10" in {
      eventCheck(expected = 206) {
        // printTraces
        countTraces must be(1)
        countEventsOf[NettyHttpReceivedStart.type] must be(1)
        countEventsOf[NettyHttpReceivedEnd.type] must be(1)
        countEventsOf[NettyPlayReceivedStart.type] must be(1)
        countEventsOf[NettyPlayReceivedEnd.type] must be(1)
        countEventsOf[ActionResolved] must be(1)
        countEventsOf[ActionRouteRequest] must be(1)
        countEventsOf[ActionInvoked] must be(1)
        countEventsOf[ActionChunkedResult] must be(1)
        countEventsOf[IterateeFolded] must be(67)
        countEventsOf[NettyResponseHeader] must be(1)
        countEventsOf[NettyWriteChunk] must be(11)
        countEventsOf[NettyReadBytes] must be(1)
        countEventsOf[ActionResultGenerationStart.type] must be(1)
        countEventsOf[ActionResultGenerationEnd.type] must be(1)
      }
    }
    "GET /get/large" in {
      eventCheck(expected = 121) {
        // printTraces
        countTraces must be(1)
        countEventsOf[NettyHttpReceivedStart.type] must be(1)
        countEventsOf[NettyHttpReceivedEnd.type] must be(1)
        countEventsOf[NettyPlayReceivedStart.type] must be(1)
        countEventsOf[NettyPlayReceivedEnd.type] must be(1)
        countEventsOf[ActionResolved] must be(1)
        countEventsOf[ActionRouteRequest] must be(1)
        countEventsOf[ActionInvoked] must be(1)
        countEventsOf[ActionSimpleResult] must be(1)
        countEventsOf[IterateeFolded] must be(38)
        countEventsOf[NettyResponseHeader] must be(1)
        countEventsOf[NettyResponseBody] must be(1)
        countEventsOf[NettyReadBytes] must be(1)
        countEventsOf[ActionResultGenerationStart.type] must be(1)
        countEventsOf[ActionResultGenerationEnd.type] must be(1)
      }
    }
    "GET /no-handler" in {
      eventCheck(expected = 115) {
        // printTraces
        countTraces must be(1)
        countEventsOf[NettyHttpReceivedStart.type] must be(1)
        countEventsOf[NettyHttpReceivedEnd.type] must be(1)
        countEventsOf[NettyPlayReceivedStart.type] must be(1)
        countEventsOf[NettyPlayReceivedEnd.type] must be(1)
        countEventsOf[ActionHandlerNotFound] must be(1)
        countEventsOf[ActionRouteRequest] must be(1)
        countEventsOf[ActionInvoked] must be(1)
        countEventsOf[ActionSimpleResult] must be(1)
        countEventsOf[IterateeFolded] must be(36)
        countEventsOf[NettyResponseHeader] must be(1)
        countEventsOf[NettyResponseBody] must be(1)
        countEventsOf[NettyReadBytes] must be(1)
        countEventsOf[ActionResultGenerationStart.type] must be(1)
        countEventsOf[ActionResultGenerationEnd.type] must be(1)
      }
    }
    "GET /error" in {
      eventCheck(expected = 109) {
        // printTraces
        countTraces must be(1)
        countEventsOf[NettyHttpReceivedStart.type] must be(1)
        countEventsOf[NettyHttpReceivedEnd.type] must be(1)
        countEventsOf[NettyPlayReceivedStart.type] must be(1)
        countEventsOf[NettyPlayReceivedEnd.type] must be(1)
        countEventsOf[ActionError] must be(1)
        countEventsOf[ActionRouteRequest] must be(1)
        countEventsOf[ActionInvoked] must be(1)
        countEventsOf[ActionSimpleResult] must be(1)
        countEventsOf[IterateeFolded] must be(35)
        countEventsOf[NettyResponseHeader] must be(1)
        countEventsOf[NettyResponseBody] must be(1)
        countEventsOf[NettyReadBytes] must be(1)
        countEventsOf[ActionResultGenerationStart.type] must be(1)
        countEventsOf[ActionResultGenerationEnd.type] must be(1)
      }
    }
    "properly trace bad requests GET /get/sync/abcd" in {
      eventCheck(expected = 121) {
        // printTraces
        countTraces must be(1)
        countEventsOf[NettyHttpReceivedStart.type] must be(1)
        countEventsOf[NettyHttpReceivedEnd.type] must be(1)
        countEventsOf[NettyPlayReceivedStart.type] must be(1)
        countEventsOf[NettyPlayReceivedEnd.type] must be(1)
        countEventsOf[ActionBadRequest] must be(1)
        countEventsOf[ActionRouteRequest] must be(1)
        countEventsOf[ActionInvoked] must be(1)
        countEventsOf[ActionSimpleResult] must be(1)
        countEventsOf[IterateeFolded] must be(38)
        countEventsOf[NettyResponseHeader] must be(1)
        countEventsOf[NettyResponseBody] must be(1)
        countEventsOf[NettyReadBytes] must be(1)
        countEventsOf[ActionResultGenerationStart.type] must be(1)
        countEventsOf[ActionResultGenerationEnd.type] must be(1)
      }
    }
  }
}

class Play21NettyPostTracingSpec extends EchoCollectSpec {
  "Play Netty (POSTs only)" must {
    "POST /post" in {
      eventCheck(expected = 277, timeout = timeoutHandler.finiteTimeoutify(10.seconds)) {
        // printTraces
        countTraces must be(1)
        countEventsOf[NettyHttpReceivedStart.type] must be(1)
        countEventsOf[NettyHttpReceivedEnd.type] must be(1)
        countEventsOf[NettyPlayReceivedStart.type] must be(1)
        countEventsOf[NettyPlayReceivedEnd.type] must be(1)
        countEventsOf[ActionResolved] must be(1)
        countEventsOf[ActionRouteRequest] must be(1)
        countEventsOf[ActionInvoked] must be(1)
        countEventsOf[ActionSimpleResult] must be(1)
        countEventsOf[NettyResponseHeader] must be(1)
        countEventsOf[NettyResponseBody] must be(1)
        countEventsOf[NettyReadBytes] must be(1)
        countEventsOf[ActionResultGenerationStart.type] must be(1)
        countEventsOf[ActionResultGenerationEnd.type] must be(1)
      }
    }
    "POST /post (BIG - should be converted by Netty to a chunked POST)" in {
      eventCheck(expected = 889, timeout = timeoutHandler.finiteTimeoutify(10.seconds)) {
        // printTraces
        countTraces must be(1)
        countEventsOf[NettyHttpReceivedStart.type] must be(1)
        countEventsOf[NettyHttpReceivedEnd.type] must be(1)
        countEventsOf[NettyPlayReceivedStart.type] must be(1)
        countEventsOf[NettyPlayReceivedEnd.type] must be(1)
        countEventsOf[ActionResolved] must be(1)
        countEventsOf[ActionRouteRequest] must be(1)
        countEventsOf[ActionInvoked] must be(1)
        countEventsOf[ActionSimpleResult] must be(1)
        countEventsOf[NettyResponseHeader] must be(1)
        countEventsOf[NettyResponseBody] must be(1)
        countEventsOf[NettyReadBytes] must be(9)
        countEventsOf[ActionResultGenerationStart.type] must be(1)
        countEventsOf[ActionResultGenerationEnd.type] must be(1)
      }
    }
    "(chunked) POST /post" in {
      eventOneOfCheck[Int](expectedOptions = Map(292 -> 2, 293 -> 3), timeout = timeoutHandler.finiteTimeoutify(10.seconds)) { readBytesCount ⇒
        // printTraces
        countTraces must be(1)
        countEventsOf[NettyHttpReceivedStart.type] must be(1)
        countEventsOf[NettyHttpReceivedEnd.type] must be(1)
        countEventsOf[NettyPlayReceivedStart.type] must be(1)
        countEventsOf[NettyPlayReceivedEnd.type] must be(1)
        countEventsOf[ActionResolved] must be(1)
        countEventsOf[ActionRouteRequest] must be(1)
        countEventsOf[ActionInvoked] must be(1)
        countEventsOf[ActionSimpleResult] must be(1)
        countEventsOf[NettyResponseHeader] must be(1)
        countEventsOf[NettyResponseBody] must be(1)
        countEventsOf[NettyReadBytes] must be(readBytesCount)
        countEventsOf[ActionResultGenerationStart.type] must be(1)
        countEventsOf[ActionResultGenerationEnd.type] must be(1)
      }
    }
    "(file upload) POST /uploadFile" in {
      eventOneOfCheck[Int](expectedOptions = Map(955 -> 6, 956 -> 5, 957 -> 4, 958 -> 3), timeout = timeoutHandler.finiteTimeoutify(10.seconds)) { readBytesCount ⇒
        // printTraces
        countTraces must be(1)
        countEventsOf[NettyHttpReceivedStart.type] must be(1)
        countEventsOf[NettyHttpReceivedEnd.type] must be(1)
        countEventsOf[NettyPlayReceivedStart.type] must be(1)
        countEventsOf[NettyPlayReceivedEnd.type] must be(1)
        countEventsOf[ActionResolved] must be(1)
        countEventsOf[ActionRouteRequest] must be(1)
        countEventsOf[ActionInvoked] must be(1)
        countEventsOf[ActionSimpleResult] must be(1)
        countEventsOf[NettyResponseHeader] must be(1)
        countEventsOf[NettyResponseBody] must be(1)
        countEventsOf[NettyReadBytes] must be(readBytesCount)
        countEventsOf[ActionResultGenerationStart.type] must be(1)
        countEventsOf[ActionResultGenerationEnd.type] must be(1)
      }
    }
    "BAD (file upload) POST /uploadFile" in {
      eventOneOfCheck[Int](expectedOptions = Map(122 -> 1, 123 -> 2, 124 -> 3, 125 -> 4, 126 -> 5), timeout = timeoutHandler.finiteTimeoutify(10.seconds)) { readBytesCount ⇒
        // printTraces
        countTraces must be(1)
        countEventsOf[NettyHttpReceivedStart.type] must be(1)
        countEventsOf[NettyHttpReceivedEnd.type] must be(1)
        countEventsOf[NettyPlayReceivedStart.type] must be(1)
        countEventsOf[NettyPlayReceivedEnd.type] must be(1)
        countEventsOf[ActionResolved] must be(1)
        countEventsOf[ActionRouteRequest] must be(1)
        countEventsOf[ActionInvoked] must be(1)
        countEventsOf[ActionSimpleResult] must be(1)
        countEventsOf[NettyResponseHeader] must be(1)
        countEventsOf[NettyResponseBody] must be(1)
        countEventsOf[NettyReadBytes] must be(readBytesCount)
        countEventsOf[ActionResultGenerationStart.type] must be(1)
        countEventsOf[ActionResultGenerationEnd.type] must be(1)
      }
    }
  }
}

class Play21IterateeTracingSpec extends EchoCollectSpec {
  "Iteratee tracing" must {
    "trace iteratees" in {
      eventCheck(expected = 17) {
        // printTraces()
        countTraces must be(1)
        countEventsOf[IterateeFolded] must be(5)
      }
    }
    "create Iteratee instances with IterateeInfo" in {
      eventCheck(expected = 1) {
        // printTraces()
        countTraces must be(1)
        countEventsOf[IterateeCreated] must be(1)
      }
    }
  }
}

class Play21NettySamplingTracingSpec extends EchoCollectSpec {
  "Play trace sampling" must {
    "produce a sample in-line with sampling rate" in {
      eventCheck(expected = 605, timeout = timeoutHandler.finiteTimeoutify(10.seconds)) {
        // printTraces
        countTraces must be(5)
        countEventsOf[NettyPlayReceivedStart.type] must be(5)
        countEventsOf[NettyPlayReceivedEnd.type] must be(5)
        countEventsOf[ActionResolved] must be(5)
        countEventsOf[ActionRouteRequest] must be(5)
        countEventsOf[ActionInvoked] must be(5)
        countEventsOf[ActionSimpleResult] must be(5)
        countEventsOf[NettyResponseHeader] must be(5)
        countEventsOf[NettyResponseBody] must be(5)
        countEventsOf[ActionResultGenerationStart.type] must be(5)
        countEventsOf[ActionResultGenerationEnd.type] must be(5)
        countEventsOf[NettyHttpReceivedStart.type] must be(5)
        countEventsOf[NettyReadBytes] must be(5)
        countEventsOf[NettyHttpReceivedEnd.type] must be(5)
      }
    }
    "not produce a useful trace when tracing disabled for a URI" in {
      eventCheck(expected = 0) {
        // allEvents.toSeq.sortBy(_.nanoTime).foreach(println)
        // printTraces
        countTraces must be(0)
      }
    }
  }
}

class Play22NettyGetTracingSpec extends EchoCollectSpec {
  "Play Netty (GETs only)" must {
    "GET /get" in {
      eventCheck(expected = 69) {
        // allEvents.toSeq.sortBy(_.nanoTime).foreach(println)
        // printTraces
        countTraces must be(1)
        countEventsOf[NettyHttpReceivedStart.type] must be(1)
        countEventsOf[NettyHttpReceivedEnd.type] must be(1)
        countEventsOf[NettyPlayReceivedStart.type] must be(1)
        countEventsOf[NettyPlayReceivedEnd.type] must be(1)
        countEventsOf[ActionResolved] must be(1)
        countEventsOf[ActionRouteRequest] must be(1)
        countEventsOf[ActionInvoked] must be(1)
        countEventsOf[ActionSimpleResult] must be(1)
        countEventsOf[NettyResponseHeader] must be(1)
        countEventsOf[NettyResponseBody] must be(1)
        countEventsOf[NettyReadBytes] must be(1)
        countEventsOf[ActionResultGenerationStart.type] must be(1)
        countEventsOf[ActionResultGenerationEnd.type] must be(1)
      }
    }
    "GET /getWithSession" in {
      eventCheck(expected = 138) {
        // printTraces
        countTraces must be(2)
        countEventsOf[NettyHttpReceivedStart.type] must be(2)
        countEventsOf[NettyHttpReceivedEnd.type] must be(2)
        countEventsOf[NettyPlayReceivedStart.type] must be(2)
        countEventsOf[NettyPlayReceivedEnd.type] must be(2)
        countEventsOf[ActionResolved] must be(2)
        countEventsOf[ActionRouteRequest] must be(2)
        countEventsOf[ActionInvoked] must be(2)
        countEventsOf[ActionSimpleResult] must be(2)
        countEventsOf[NettyResponseHeader] must be(2)
        countEventsOf[NettyResponseBody] must be(2)
        countEventsOf[NettyReadBytes] must be(2)
        countEventsOf[ActionResultGenerationStart.type] must be(2)
        countEventsOf[ActionResultGenerationEnd.type] must be(2)
        eventsOf[ActionInvoked].exists(_.annotation.asInstanceOf[ActionInvoked].invocationInfo.session == Some(Map("key" -> "value"))) must be(true)
      }
    }
    "GET /get/sync/10" in {
      eventCheck(expected = 69, timeout = timeoutHandler.finiteTimeoutify(10.seconds)) {
        // printTraces
        countTraces must be(1)
        countEventsOf[NettyHttpReceivedStart.type] must be(1)
        countEventsOf[NettyHttpReceivedEnd.type] must be(1)
        countEventsOf[NettyPlayReceivedStart.type] must be(1)
        countEventsOf[NettyPlayReceivedEnd.type] must be(1)
        countEventsOf[ActionResolved] must be(1)
        countEventsOf[ActionRouteRequest] must be(1)
        countEventsOf[ActionInvoked] must be(1)
        countEventsOf[ActionSimpleResult] must be(1)
        countEventsOf[NettyResponseHeader] must be(1)
        countEventsOf[NettyResponseBody] must be(1)
        countEventsOf[NettyReadBytes] must be(1)
        countEventsOf[ActionResultGenerationStart.type] must be(1)
        countEventsOf[ActionResultGenerationEnd.type] must be(1)
      }
    }
    "GET /get/async/100 -- 100ms delay" in {
      eventCheck(expected = 70) {
        // printTraces
        countTraces must be(1)
        countEventsOf[NettyHttpReceivedStart.type] must be(1)
        countEventsOf[NettyHttpReceivedEnd.type] must be(1)
        countEventsOf[NettyPlayReceivedStart.type] must be(1)
        countEventsOf[NettyPlayReceivedEnd.type] must be(1)
        countEventsOf[ActionResolved] must be(1)
        countEventsOf[ActionRouteRequest] must be(1)
        countEventsOf[ActionInvoked] must be(1)
        countEventsOf[ActionSimpleResult] must be(1)
        countEventsOf[ActionAsyncResult.type] must be(1)
        countEventsOf[NettyResponseHeader] must be(1)
        countEventsOf[NettyResponseBody] must be(1)
        countEventsOf[NettyReadBytes] must be(1)
        countEventsOf[ActionResultGenerationStart.type] must be(1)
        countEventsOf[ActionResultGenerationEnd.type] must be(1)
      }
    }
    "GET /get/async_immediate -- no delay" in {
      eventCheck(expected = 70) {
        // printTraces
        countTraces must be(1)
        countEventsOf[NettyHttpReceivedStart.type] must be(1)
        countEventsOf[NettyHttpReceivedEnd.type] must be(1)
        countEventsOf[NettyPlayReceivedStart.type] must be(1)
        countEventsOf[NettyPlayReceivedEnd.type] must be(1)
        countEventsOf[ActionResolved] must be(1)
        countEventsOf[ActionRouteRequest] must be(1)
        countEventsOf[ActionInvoked] must be(1)
        countEventsOf[ActionSimpleResult] must be(1)
        countEventsOf[ActionAsyncResult.type] must be(1)
        countEventsOf[NettyResponseHeader] must be(1)
        countEventsOf[NettyResponseBody] must be(1)
        countEventsOf[NettyReadBytes] must be(1)
        countEventsOf[ActionResultGenerationStart.type] must be(1)
        countEventsOf[ActionResultGenerationEnd.type] must be(1)
      }
    }
    "GET /get/chunked/10" in {
      eventCheck(expected = 759, timeout = timeoutHandler.finiteTimeoutify(10.seconds)) {
        // printTraces
        // Unable to get stable data out of this
        countTraces must be(1)
        countEventsOf[NettyHttpReceivedStart.type] must be(1)
        countEventsOf[NettyHttpReceivedEnd.type] must be(1)
        countEventsOf[NettyPlayReceivedStart.type] must be(1)
        countEventsOf[NettyPlayReceivedEnd.type] must be(1)
        countEventsOf[ActionResolved] must be(1)
        countEventsOf[ActionRouteRequest] must be(1)
        countEventsOf[ActionInvoked] must be(1)
        countEventsOf[ActionChunkedResult] must be(1)
        countEventsOf[IterateeFolded] must be(245)
        countEventsOf[NettyResponseHeader] must be(1)
        countEventsOf[NettyWriteChunk] must be(11)
        countEventsOf[NettyReadBytes] must be(1)
        countEventsOf[ActionResultGenerationStart.type] must be(1)
        countEventsOf[ActionResultGenerationEnd.type] must be(1)
      }
    }
    "GET /get/large" in {
      eventCheck(expected = 69, timeout = timeoutHandler.finiteTimeoutify(10.seconds)) {
        // printTraces
        countTraces must be(1)
        countEventsOf[NettyHttpReceivedStart.type] must be(1)
        countEventsOf[NettyHttpReceivedEnd.type] must be(1)
        countEventsOf[NettyPlayReceivedStart.type] must be(1)
        countEventsOf[NettyPlayReceivedEnd.type] must be(1)
        countEventsOf[ActionResolved] must be(1)
        countEventsOf[ActionRouteRequest] must be(1)
        countEventsOf[ActionInvoked] must be(1)
        countEventsOf[ActionSimpleResult] must be(1)
        countEventsOf[IterateeFolded] must be(19)
        countEventsOf[NettyResponseHeader] must be(1)
        countEventsOf[NettyResponseBody] must be(1)
        countEventsOf[NettyReadBytes] must be(1)
        countEventsOf[ActionResultGenerationStart.type] must be(1)
        countEventsOf[ActionResultGenerationEnd.type] must be(1)
      }
    }
    "GET /no-handler" in {
      eventCheck(expected = 57) {
        // printTraces
        countTraces must be(1)
        countEventsOf[NettyHttpReceivedStart.type] must be(1)
        countEventsOf[NettyHttpReceivedEnd.type] must be(1)
        countEventsOf[NettyPlayReceivedStart.type] must be(1)
        countEventsOf[NettyPlayReceivedEnd.type] must be(1)
        countEventsOf[ActionHandlerNotFound] must be(1)
        countEventsOf[ActionRouteRequest] must be(1)
        countEventsOf[ActionInvoked] must be(1)
        countEventsOf[ActionSimpleResult] must be(1)
        countEventsOf[IterateeFolded] must be(15)
        countEventsOf[NettyResponseHeader] must be(1)
        countEventsOf[NettyResponseBody] must be(1)
        countEventsOf[NettyReadBytes] must be(1)
        countEventsOf[ActionResultGenerationStart.type] must be(1)
        countEventsOf[ActionResultGenerationEnd.type] must be(1)
      }
    }
    "GET /error" in {
      eventCheck(expected = 69, timeout = timeoutHandler.finiteTimeoutify(10.seconds)) {
        // printTraces
        countTraces must be(1)
        countEventsOf[NettyHttpReceivedStart.type] must be(1)
        countEventsOf[NettyHttpReceivedEnd.type] must be(1)
        countEventsOf[NettyPlayReceivedStart.type] must be(1)
        countEventsOf[NettyPlayReceivedEnd.type] must be(1)
        countEventsOf[ActionError] must be(1)
        countEventsOf[ActionRouteRequest] must be(1)
        countEventsOf[ActionInvoked] must be(1)
        countEventsOf[ActionSimpleResult] must be(1)
        countEventsOf[IterateeFolded] must be(20)
        countEventsOf[NettyResponseHeader] must be(1)
        countEventsOf[NettyResponseBody] must be(1)
        countEventsOf[NettyReadBytes] must be(1)
        countEventsOf[ActionResultGenerationStart.type] must be(1)
        countEventsOf[ActionResultGenerationEnd.type] must be(1)
      }
    }
    "properly trace bad requests GET /get/sync/abcd" in {
      eventCheck(expected = 69) {
        // printTraces
        countTraces must be(1)
        countEventsOf[NettyHttpReceivedStart.type] must be(1)
        countEventsOf[NettyHttpReceivedEnd.type] must be(1)
        countEventsOf[NettyPlayReceivedStart.type] must be(1)
        countEventsOf[NettyPlayReceivedEnd.type] must be(1)
        countEventsOf[ActionBadRequest] must be(1)
        countEventsOf[ActionRouteRequest] must be(1)
        countEventsOf[ActionInvoked] must be(1)
        countEventsOf[ActionSimpleResult] must be(1)
        countEventsOf[IterateeFolded] must be(19)
        countEventsOf[NettyResponseHeader] must be(1)
        countEventsOf[NettyResponseBody] must be(1)
        countEventsOf[NettyReadBytes] must be(1)
        countEventsOf[ActionResultGenerationStart.type] must be(1)
        countEventsOf[ActionResultGenerationEnd.type] must be(1)
      }
    }
  }
}

class Play22NettyPostTracingSpec extends EchoCollectSpec {
  "Play Netty (POSTs only)" must {
    "POST /post" in {
      eventCheck(expected = 214) {
        // printTraces
        countTraces must be(1)
        countEventsOf[NettyHttpReceivedStart.type] must be(1)
        countEventsOf[NettyHttpReceivedEnd.type] must be(1)
        countEventsOf[NettyPlayReceivedStart.type] must be(1)
        countEventsOf[NettyPlayReceivedEnd.type] must be(1)
        countEventsOf[ActionResolved] must be(1)
        countEventsOf[ActionRouteRequest] must be(1)
        countEventsOf[ActionInvoked] must be(1)
        countEventsOf[ActionSimpleResult] must be(1)
        countEventsOf[NettyResponseHeader] must be(1)
        countEventsOf[NettyResponseBody] must be(1)
        countEventsOf[NettyReadBytes] must be(1)
        countEventsOf[ActionResultGenerationStart.type] must be(1)
        countEventsOf[ActionResultGenerationEnd.type] must be(1)
      }
    }
    "POST /post (BIG - should be converted by Netty to a chunked POST)" in {
      eventCheck(expected = 771, timeout = timeoutHandler.finiteTimeoutify(10.seconds)) {
        // printTraces
        countTraces must be(1)
        countEventsOf[NettyHttpReceivedStart.type] must be(1)
        countEventsOf[NettyHttpReceivedEnd.type] must be(1)
        countEventsOf[NettyPlayReceivedStart.type] must be(1)
        countEventsOf[NettyPlayReceivedEnd.type] must be(1)
        countEventsOf[ActionResolved] must be(1)
        countEventsOf[ActionRouteRequest] must be(1)
        countEventsOf[ActionInvoked] must be(1)
        countEventsOf[ActionSimpleResult] must be(1)
        countEventsOf[NettyResponseHeader] must be(1)
        countEventsOf[NettyResponseBody] must be(1)
        countEventsOf[NettyReadBytes] must be(9)
        countEventsOf[ActionResultGenerationStart.type] must be(1)
        countEventsOf[ActionResultGenerationEnd.type] must be(1)
      }
    }
    "(chunked) POST /post" in {
      eventOneOfCheck[Int](expectedOptions = Map(229 -> 2, 230 -> 3), timeout = timeoutHandler.finiteTimeoutify(10.seconds)) { readBytesCount ⇒
        // printTraces
        countTraces must be(1)
        countEventsOf[NettyHttpReceivedStart.type] must be(1)
        countEventsOf[NettyHttpReceivedEnd.type] must be(1)
        countEventsOf[NettyPlayReceivedStart.type] must be(1)
        countEventsOf[NettyPlayReceivedEnd.type] must be(1)
        countEventsOf[ActionResolved] must be(1)
        countEventsOf[ActionRouteRequest] must be(1)
        countEventsOf[ActionInvoked] must be(1)
        countEventsOf[ActionSimpleResult] must be(1)
        countEventsOf[NettyResponseHeader] must be(1)
        countEventsOf[NettyResponseBody] must be(1)
        countEventsOf[NettyReadBytes] must be(readBytesCount)
        countEventsOf[ActionResultGenerationStart.type] must be(1)
        countEventsOf[ActionResultGenerationEnd.type] must be(1)
      }
    }
    "(file upload) POST /uploadFile" in {
      eventOneOfCheck[Int](expectedOptions = Map(864 -> 4, 865 -> 5, 866 -> 6, 867 -> 7), timeout = timeoutHandler.finiteTimeoutify(10.seconds)) { readBytesCount ⇒
        // printTraces
        countTraces must be(1)
        countEventsOf[NettyHttpReceivedStart.type] must be(1)
        countEventsOf[NettyHttpReceivedEnd.type] must be(1)
        countEventsOf[NettyPlayReceivedStart.type] must be(1)
        countEventsOf[NettyPlayReceivedEnd.type] must be(1)
        countEventsOf[ActionResolved] must be(1)
        countEventsOf[ActionRouteRequest] must be(1)
        countEventsOf[ActionInvoked] must be(1)
        countEventsOf[ActionSimpleResult] must be(1)
        countEventsOf[NettyResponseHeader] must be(1)
        countEventsOf[NettyResponseBody] must be(1)
        countEventsOf[NettyReadBytes] must be(readBytesCount)
        countEventsOf[ActionResultGenerationStart.type] must be(1)
        countEventsOf[ActionResultGenerationEnd.type] must be(1)
      }
    }
    "BAD (file upload) POST /uploadFile" in {
      eventOneOfCheck[Int](expectedOptions = Map(79 -> 2, 80 -> 3, 81 -> 4, 82 -> 5), timeout = timeoutHandler.finiteTimeoutify(10.seconds)) { readBytesCount ⇒
        // printTraces
        countTraces must be(1)
        countEventsOf[NettyHttpReceivedStart.type] must be(1)
        countEventsOf[NettyHttpReceivedEnd.type] must be(1)
        countEventsOf[NettyPlayReceivedStart.type] must be(1)
        countEventsOf[NettyPlayReceivedEnd.type] must be(1)
        countEventsOf[ActionResolved] must be(1)
        countEventsOf[ActionRouteRequest] must be(1)
        countEventsOf[ActionInvoked] must be(1)
        countEventsOf[ActionSimpleResult] must be(1)
        countEventsOf[NettyResponseHeader] must be(1)
        countEventsOf[NettyResponseBody] must be(1)
        countEventsOf[NettyReadBytes] must be(readBytesCount)
        countEventsOf[ActionResultGenerationStart.type] must be(1)
        countEventsOf[ActionResultGenerationEnd.type] must be(1)
      }
    }
  }
}

class Play22IterateeTracingSpec extends EchoCollectSpec {
  "Iteratee tracing" must {
    "trace iteratees" in {
      eventCheck(expected = 24) {
        // printTraces()
        // countTraces must be(1)
        countEventsOf[IterateeFolded] must be(7)
      }
    }
    "create Iteratee instances with IterateeInfo" in {
      eventCheck(expected = 1) {
        // printTraces()
        // countTraces must be(1)
        countEventsOf[IterateeCreated] must be(1)
      }
    }
  }
}

class Play22NettySamplingTracingSpec extends EchoCollectSpec {
  "Play trace sampling" must {
    "produce a sample in-line with sampling rate" in {
      eventCheck(expected = 345) {
        // printTraces
        countTraces must be(5)
        countEventsOf[NettyPlayReceivedStart.type] must be(5)
        countEventsOf[NettyPlayReceivedEnd.type] must be(5)
        countEventsOf[ActionResolved] must be(5)
        countEventsOf[ActionRouteRequest] must be(5)
        countEventsOf[ActionInvoked] must be(5)
        countEventsOf[ActionSimpleResult] must be(5)
        countEventsOf[NettyResponseHeader] must be(5)
        countEventsOf[NettyResponseBody] must be(5)
        countEventsOf[ActionResultGenerationStart.type] must be(5)
        countEventsOf[ActionResultGenerationEnd.type] must be(5)
        countEventsOf[NettyHttpReceivedStart.type] must be(5)
        countEventsOf[NettyReadBytes] must be(5)
        countEventsOf[NettyHttpReceivedEnd.type] must be(5)
      }
    }
    "not produce a useful trace when tracing disabled for a URI" in {
      eventCheck(expected = 0) {
        // allEvents.toSeq.sortBy(_.nanoTime).foreach(println)
        // printTraces
        countTraces must be(0)
      }
    }
  }
}
