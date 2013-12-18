/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */

package com.typesafe.trace

import com.typesafe.trace.test.EchoCollectSpec
import scala.concurrent.duration._

class Play21NettyGetTracingSpec extends EchoCollectSpec {
  "Play Netty (GETs only)" must {
    "GET /get" in {
      eventCheck(expected = 416) {
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
      eventCheck(expected = 832) {
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
      eventCheck(expected = 416) {
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
      eventCheck(expected = 440) {
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
      eventCheck(expected = 440) {
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
      eventCheck(expected = 795) {
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
      eventCheck(expected = 416) {
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
      eventCheck(expected = 400) {
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
      eventCheck(expected = 389) {
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
      eventCheck(expected = 416) {
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
      eventCheck(expected = 813) {
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
      eventCheck(expected = 2530) {
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
      eventOneOfCheck(expectedOptions = Map(855 -> 4, 854 -> 3, 853 -> 2), timeout = 10.seconds) { readBytesCount ⇒
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
      eventOneOfCheck[Int](expectedOptions = Map(2622 -> 6, 2621 -> 5, 2620 -> 4), timeout = 10.seconds) { readBytesCount ⇒
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
      eventOneOfCheck[Int](expectedOptions = Map(421 -> 5, 420 -> 4, 419 -> 3, 418 -> 2, 417 -> 1), timeout = 10.seconds) { readBytesCount ⇒
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
      eventCheck(expected = 59) {
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
      eventCheck(expected = 2080) {
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
      eventCheck(expected = 369) {
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
      eventCheck(expected = 738) {
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
      eventCheck(expected = 369) {
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
      eventCheck(expected = 383) {
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
      eventCheck(expected = 383) {
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
      eventCheck(expected = 3159) {
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
        countEventsOf[IterateeFolded] must be(184)
        countEventsOf[NettyResponseHeader] must be(1)
        countEventsOf[NettyWriteChunk] must be(11)
        countEventsOf[NettyReadBytes] must be(1)
        countEventsOf[ActionResultGenerationStart.type] must be(1)
        countEventsOf[ActionResultGenerationEnd.type] must be(1)
      }
    }
    "GET /get/large" in {
      eventCheck(expected = 369) {
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
      eventCheck(expected = 321) {
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
      eventCheck(expected = 367) {
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
      eventCheck(expected = 369) {
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
      eventCheck(expected = 1050) {
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
      eventCheck(expected = 3968) {
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
      eventOneOfCheck(expectedOptions = Map(1125 -> 4, 1124 -> 3, 1123 -> 2), timeout = 10.seconds) { readBytesCount ⇒
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
      eventOneOfCheck[Int](expectedOptions = Map(4243 -> 7, 4242 -> 6, 4241 -> 5, 4240 -> 4, 4219 -> 3), timeout = 10.seconds) { readBytesCount ⇒
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
      eventOneOfCheck[Int](expectedOptions = Map(422 -> 7, 421 -> 6, 420 -> 5, 419 -> 4, 418 -> 3, 417 -> 2, 416 -> 1), timeout = 10.seconds) { readBytesCount ⇒
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
      eventCheck(expected = 205) {
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
      eventCheck(expected = 1845) {
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
