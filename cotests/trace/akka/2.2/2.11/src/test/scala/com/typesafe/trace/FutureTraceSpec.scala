/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */

package com.typesafe.trace

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.trace.test.{ TimeoutHandler, EchoTraceSpec }
import com.typesafe.trace.util.ExpectedFailureException
import java.util.concurrent.{ CountDownLatch, TimeoutException }
import scala.concurrent.duration._
import scala.concurrent.{ Await, Future, Promise }

object FutureTraceSpec {
  case class Payload(
    msgLatch: CountDownLatch,
    sendException: Boolean = false,
    replyBefore: Boolean = false,
    timeout: Boolean = false)

  case class AskBlocking(payload: Payload, actor: ActorRef)
  case class AskWithCallback(payload: Payload, actor: ActorRef)
  case class Question(payload: Payload)
  case class Reply(payload: Payload)
  case class ResultFromCallback(payload: Payload)
  case class ExceptionFromCallback(payload: Payload)
  case class TimeoutFromCallback(payload: Payload)
  case class UseFuturesInsideActor(payload: Payload)
  case object WhoAreYou

  class TestActor extends Actor {

    import context.dispatcher

    val timeoutHandler = TimeoutHandler(context.system.settings.config.getInt("activator.trace.test.time-factor"))

    def receive = {
      case AskBlocking(payload, actor) ⇒
        implicit val timeout = Timeout(timeoutHandler.time, timeoutHandler.unit)
        val future = actor ? Question(payload)

        if (payload.replyBefore) Thread.sleep(500)
        if (payload.timeout) {
          try {
            Await.ready(future, 1.second)
          } catch {
            case _: TimeoutException ⇒ payload.msgLatch.countDown()
          }
        } else {
          Await.ready(future, timeout.duration)
          payload.msgLatch.countDown()
        }

      case AskWithCallback(payload, actor) ⇒
        implicit val timeout = if (payload.timeout) Timeout((timeoutHandler.factor * 1).seconds) else Timeout(timeoutHandler.time, timeoutHandler.unit)
        val future = actor ? Question(payload)

        if (payload.replyBefore) Thread.sleep(500)
        future onSuccess {
          case Reply(msgLatch) ⇒ self ! ResultFromCallback(msgLatch)
        }
        future onFailure {
          case _: RuntimeException ⇒ self ! ExceptionFromCallback(payload)
        }

        if (payload.timeout) {
          future onFailure {
            case _: TimeoutException ⇒ self ! TimeoutFromCallback(payload)
          }
        }

      case Question(payload) ⇒
        if (payload.timeout) Thread.sleep(1010)
        if (!payload.replyBefore) Thread.sleep(500)
        if (payload.sendException) {
          sender ! Status.Failure(new ExpectedFailureException("Simulated"))
        } else {
          sender ! Reply(payload)
        }

      case ResultFromCallback(payload) ⇒
        payload.msgLatch.countDown()

      case ExceptionFromCallback(payload) ⇒
        payload.msgLatch.countDown()

      case TimeoutFromCallback(payload) ⇒
        payload.msgLatch.countDown()

      case UseFuturesInsideActor(payload) ⇒
        val future = Future {
          self ! ResultFromCallback(payload)
          "Hello" + "World"
        }
        Await.ready(future, timeoutHandler.duration)

      case WhoAreYou ⇒ sender ! self
    }
  }
}

class Akka22Scala211FutureTraceSpec extends EchoTraceSpec {
  import FutureTraceSpec._

  var actor1: ActorRef = _
  var actor2: ActorRef = _

  override def beforeAll() = {
    System.setProperty("activator.trace.futures", "on")
    super.beforeAll()
  }

  override def beforeEach() = {
    super.beforeEach()
    barrier("create-actors")
    actor1 = system.actorOf(Props[TestActor], "actor1")
    actor2 = system.actorOf(Props[TestActor], "actor2")
    eventCheck("create-actors")
  }

  override def afterEach() = {
    barrier("poison")
    actor1 ! PoisonPill
    actor2 ! PoisonPill
    eventCheck("poison")
    super.afterEach()
  }

  "FutureTrace" must {

    "trace actors that ask blocking" in {
      val latch = new CountDownLatch(1)
      val payload = Payload(latch)
      actor1 ! AskBlocking(payload, actor2)

      latch.await(timeoutHandler.time, timeoutHandler.unit) must be(true)

      eventCheck()
    }

    "trace actors that ask blocking with await after reply" in {
      val latch = new CountDownLatch(1)
      val payload = Payload(latch, replyBefore = true)
      actor1 ! AskBlocking(payload, actor2)

      latch.await(timeoutHandler.time, timeoutHandler.unit) must be(true)

      eventCheck()
    }

    "trace actors that ask blocking and receiver times out" in {
      val latch = new CountDownLatch(1)
      val payload = Payload(latch, timeout = true)
      actor1 ! AskBlocking(payload, actor2)

      latch.await(timeoutHandler.time, timeoutHandler.unit) must be(true)

      eventCheck()
    }

    "trace actors that ask blocking and receiver replies with exception" in {
      val latch = new CountDownLatch(1)
      val payload = Payload(latch, sendException = true)
      actor1 ! AskBlocking(payload, actor2)

      latch.await(timeoutHandler.time, timeoutHandler.unit) must be(true)

      eventCheck()
    }

    "trace actors that ask with callback" in {
      val latch = new CountDownLatch(1)
      val payload = Payload(latch)
      actor1 ! AskWithCallback(payload, actor2)

      latch.await(timeoutHandler.time, timeoutHandler.unit) must be(true)

      eventCheck()
    }

    "trace actors that ask with callback added after reply" in {
      val latch = new CountDownLatch(1)
      val payload = Payload(latch, replyBefore = true)
      actor1 ! AskWithCallback(payload, actor2)

      latch.await(timeoutHandler.time, timeoutHandler.unit) must be(true)

      eventCheck()
    }

    "trace actors that ask with callback and receiver replies with exception" in {
      val latch = new CountDownLatch(1)
      val payload = Payload(latch, sendException = true)
      actor1 ! AskWithCallback(payload, actor2)

      latch.await(timeoutHandler.time, timeoutHandler.unit) must be(true)

      eventCheck()
    }

    "trace actors that ask with callback added after reply and receiver replies with exception" in {
      val latch = new CountDownLatch(1)
      val payload = Payload(latch, sendException = true, replyBefore = true)
      actor1 ! AskWithCallback(payload, actor2)

      latch.await(timeoutHandler.time, timeoutHandler.unit) must be(true)

      eventCheck()
    }

    "trace actors that ask with callback and receiver times out" in {
      val latch = new CountDownLatch(1)
      val payload = Payload(latch, timeout = true)
      actor1 ! AskWithCallback(payload, actor2)

      latch.await(timeoutHandler.time, timeoutHandler.unit) must be(true)

      eventCheck()
    }

    "trace futures that are used outside actors" in {
      implicit val executionContext = system.dispatcher

      val future = Future {
        "Hello" + "World"
      }

      Await.ready(future, timeoutHandler.duration)

      eventCheck()
    }

    "trace futures that are used inside actor" in {
      val latch = new CountDownLatch(1)
      val payload = Payload(latch)
      actor1 ! UseFuturesInsideActor(payload)

      latch.await(timeoutHandler.time, timeoutHandler.unit) must be(true)

      eventCheck()
    }

    "trace kept promises" in {
      implicit val executionContext = system.dispatcher

      val successful = Future.successful(42)
      val failed = Future.failed(new ExpectedFailureException("failed"))

      successful onComplete { _ ⇒ () }
      failed onComplete { _ ⇒ () }

      eventCheck()
    }

    "record trace info of future results" in {
      implicit val timeout = Timeout(timeoutHandler.time, timeoutHandler.unit)

      actor1 ? WhoAreYou

      eventCheck()
    }

  }
}
