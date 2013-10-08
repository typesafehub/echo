/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */

package com.typesafe.atmos.trace

import play.api.libs.ws.WS
import play.api.test.Helpers._
import play.api.test.TestBrowser
import com.typesafe.atmos.test.AsyncHttpHelpers
import java.io.{ ByteArrayInputStream, File }
import com.ning.http.client.generators.InputStreamBodyGenerator
import com.ning.http.client.FilePart
import scala.collection.JavaConverters._

object CookieParser {
  // quick and dirty.  AsyncHttpClient fails to parse the Play cookie correctly
  val playCookie = """.*PLAY_SESSION="(.*)";.*""".r
}

class Play21IterateeTracingSpec extends ActionTraceSpec {
  "Iteratee tracing" must {
    import com.typesafe.atmos.trace.ActionTracer
    import play.api.libs.iteratee._
    import scala.concurrent.Future
    "trace iteratees" in {
      ActionTracer.global.group("iteratee") {
        val enumerator = Enumerator(1, 2, 3)
        enumerator.run(Iteratee.fold(0) { (s, x) ⇒ s + x })
      }
      eventCheck()
    }
    "create Iteratee instances with IterateeInfo" in {
      val it = new Iteratee[Nothing, Nothing] {
        def fold[B](folder: Step[Nothing, Nothing] ⇒ Future[B]): Future[B] = null
      }
      IterateeTrace.iterateeInfo(it).isInstanceOf[IterateeInfo] must be(true)
      eventCheck()
    }
  }
}

class Play21NettyGetTracingSpec extends ActionTraceNettySpec {
  import CookieParser._
  "Play Netty (GETs only)" must {
    "GET /get" in {
      val r = await(WS.url("http://localhost:9876/get").get)
      r.status must be(OK)
      // println("********** BODY:" + r.body)
      eventCheck()
    }
    "GET /getWithSession" in {
      val r = await(WS.url("http://localhost:9876/getWithSession").get)
      r.status must be(OK)
      val cookies: String = r.getAHCResponse.getHeaders("Set-Cookie").asScala.flatMap(_ match {
        case playCookie(cookie) ⇒ Seq("PLAY_SESSION=\"" + cookie + "\"")
        case _                  ⇒ Seq()
      }).mkString("; ")
      val r1 = await(WS.url("http://localhost:9876/getWithSession").withHeaders("Cookie" -> cookies).get)
      r1.status must be(OK)
      // println("********** BODY:" + r1.body)
      eventCheck()
    }
    "GET /get/sync/10" in {
      val r = await(WS.url("http://localhost:9876/get/sync/10").get)
      r.status must be(OK)
      // println("********** BODY:" + r.body)
      eventCheck()
    }
    "GET /get/async/100 -- 100ms delay" in {
      val r = await(WS.url("http://localhost:9876/get/async/100").get)
      r.status must be(OK)
      // println("********** BODY:" + r.body)
      eventCheck()
    }
    "GET /get/async_immediate -- no delay" in {
      val r = await(WS.url("http://localhost:9876/get/async_immediate").get)
      r.status must be(OK)
      // println("********** BODY:" + r.body)
      eventCheck()
    }
    "GET /get/chunked/10" in {
      val r = await(WS.url("http://localhost:9876/get/chunked/10").get)
      r.status must be(OK)
      // println("********** BODY:" + r.body)
      eventCheck()
    }
    "GET /get/large" in {
      val r = await(WS.url("http://localhost:9876/get/large").get)
      r.status must be(OK)
      // println("********** BODY:" + r.body)
      eventCheck()
    }
    "GET /no-handler" in {
      val r = await(WS.url("http://localhost:9876/no-handler").get)
      r.status must not be (OK)
      // println("********** BODY:" + r.body)
      eventCheck()
    }
    "GET /error" in {
      val r = await(WS.url("http://localhost:9876/error").get)
      r.status must not be (OK)
      // println("********** BODY:" + r.body)
      eventCheck()
    }
    "properly trace bad requests GET /get/sync/abcd" in {
      val r = await(WS.url("http://localhost:9876/get/sync/abcd").get)
      r.status must not be (OK)
      // println("********** BODY:" + r.body)
      eventCheck()
    }
  }
}

class Play21NettyPostTracingSpec extends ActionTraceNettySpec {
  import AsyncHttpHelpers._

  "Play Netty (POSTs only)" must {
    "POST /post" in {
      val r = await(WS.url("http://localhost:9876/post").post(Map("key1" -> Seq("value1"))))
      r.status must be(OK)
      // println("********** BODY:" + r.body)
      eventCheck()
    }
    "(chunked) POST /post" in {

      val body = new ByteArrayInputStream(urlEncodeForm(Map("key1" -> Seq("value1"),
        "key2" -> Seq("value2"),
        "key3" -> Seq("value3"),
        "key4" -> Seq("value4"),
        "key5" -> Seq("value5"),
        "key6" -> Seq("value6"),
        "key7" -> Seq("value7"),
        "key8" -> Seq("value8"),
        "key9" -> Seq("value9"),
        "key10" -> Seq("value10"))).getBytes("utf-8"))
      val request = requestBuilder("POST", "http://localhost:9876/post")
      request.setBody(new InputStreamBodyGenerator(body))
      request.setHeader("Content-Type", "application/x-www-form-urlencoded")
      val c = client
      val response = c.executeRequest(request.build()).get()
      response.getStatusCode must be(200)
      c.close()
      // println("********** BODY:" + response)
      eventCheck()
    }
    "(file upload) POST /uploadFile" in {
      val dir = System.getProperty("user.dir")
      val request = requestBuilder("POST", "http://localhost:9876/uploadFile")
      val part = new FilePart("picture.txt",
        new File(dir + "/cotests/trace/play/common/src/test/sampleData/picture.txt"),
        "text/plain",
        "UTF-8")
      request.addBodyPart(part)
      request.setHeader("Content-Type", "multipart/form-data")
      val c = client
      val response = c.executeRequest(request.build()).get()
      // println("********** BODY:" + response.getResponseBody)
      response.getStatusCode must be(200)
      c.close()
      eventCheck()
    }
    "BAD (file upload) POST /uploadFile" in {
      val dir = System.getProperty("user.dir")
      val r = await(WS.url("http://localhost:9876/uploadFile").post(new File(dir + "/cotests/trace/play/common/src/test/sampleData/picture.txt")))
      // println("********** BODY:" + r.body)
      r.status must be(BAD_REQUEST)
      eventCheck()
    }
  }
}

class Play21NettySamplingTracingSpec extends ActionTraceNettySpec {
  "Play trace sampling" must {
    "produce a sample in-line with sampling rate" in {
      for (_ ← 1 to 13) { // 13 requests only 5 "full" traces
        val r = await(WS.url("http://localhost:9876/getSampled").get)
        r.status must be(OK)
        // println("********** BODY:" + r1.body)
      }
      eventCheck()
    }
    "not produce a useful trace when tracing disabled for a URI" in {
      val r = await(WS.url("http://localhost:9876/get/filtered/10").get)
      r.status must be(OK)
      // println("********** BODY:" + r.body)
      eventCheck()
    }
  }
}
