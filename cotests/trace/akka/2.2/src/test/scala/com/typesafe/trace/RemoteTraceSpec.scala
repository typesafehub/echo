/**
 *  Copyright (C) 2009-2012 Typesafe Inc. <http://www.typesafe.com>
 */

package com.typesafe.trace

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.trace.test.{ TimeoutHandler, EchoTraceSpec }
import com.typesafe.trace.uuid.UUID
import java.util.concurrent.CountDownLatch
import scala.concurrent.Await
import scala.concurrent.duration._

object RemoteTraceTest {
  val Host = "localhost"
  val Port1 = 9901
  val Port2 = 9902

  val config1 = config(Host, Port1)
  val config2 = config(Host, Port2)

  def config(host: String, port: Int): String = """
    akka {
      loglevel = WARNING
      logger-startup-timeout = 10s
      actor {
        provider = "akka.remote.RemoteActorRefProvider"
      }
      remote {
        enabled-transports = ["akka.remote.netty.tcp"]
        netty.tcp {
          hostname = "%s"
          port = %s
        }
        log-remote-lifecycle-events = off
      }
    }
    activator.trace {
      remote-life-cycle = off
      traceable {
        "/" = on
        "*" = on
      }
    }
  """.format(host, port)

  case class Message(n: Int, trace: Option[UUID])
  case class Question(n: Int)
  case object GetTraces

  class ActorA(actorB: ActorRef) extends Actor {
    var latch: Option[CountDownLatch] = None
    var trace1: Option[UUID] = None
    var trace2: Option[UUID] = None
    var trace4: Option[UUID] = None

    val timeoutHandler = TimeoutHandler(context.system.settings.config.getInt("activator.trace.test.time-factor"))

    def receive = {
      case cdl: CountDownLatch ⇒
        latch = Some(cdl)
      case Message(n, sentTrace) ⇒
        if (n == 1) {
          trace1 = sentTrace
          trace2 = currentTrace(context.system)
          implicit val timeout = Timeout(timeoutHandler.time, timeoutHandler.unit)
          Await.ready(actorB ? Question(n), timeout.duration)
          actorB ! Message(2, trace2)
        } else {
          trace4 = sentTrace
          latch.foreach(_.countDown)
        }
      case GetTraces ⇒ sender ! List(trace1, trace2, trace4).flatten.toSet
    }
  }

  class ActorB extends Actor {
    var trace2: Option[UUID] = None
    var trace3: Option[UUID] = None

    def receive = {
      case Message(n, sentTrace) ⇒
        trace2 = sentTrace
        trace3 = currentTrace(context.system)
        val actorC = context.actorOf(Props[ActorC], "c")
        actorC forward Message(3, trace3)
        actorC ! PoisonPill
      case Question(n) ⇒ sender ! ("Answer: " + n * 2)
      case GetTraces   ⇒ sender ! List(trace2, trace3).flatten.toSet
    }
  }

  class ActorC extends Actor {
    def receive = {
      case Message(n, sentTrace) ⇒ sender ! Message(4, currentTrace(context.system))
    }
  }

  def currentTrace(system: ActorSystem): Option[UUID] = {
    ActorSystemTracer(system).trace.local.current.map(_.context.trace)
  }
}

class Akka22RemoteTraceSpec extends EchoTraceSpec(RemoteTraceTest.config1) {
  import RemoteTraceTest._

  override def cotestNodes = 3

  "Remote tracing" must {

    "trace across nodes" in {
      barrier("setup")

      implicit val timeout = Timeout(timeoutHandler.time, timeoutHandler.unit)

      val remoteAddress = "akka.tcp://Akka22RemoteTraceSpec@%s:%s/user/%s".format(Host, Port2, "b")
      val actorBSelection = system.actorSelection(remoteAddress)
      val actorBIdentity = Await.result(actorBSelection ? Identify(remoteAddress), timeout.duration)
      val ActorIdentity(_, Some(actorB)) = actorBIdentity
      val actorA = system.actorOf(Props(new ActorA(actorB)), "a")

      eventCheck("create-and-identify")

      barrier("start")

      Tracer(system).startGroup("test")
      val trace = currentTrace(system)
      val latch = new CountDownLatch(1)
      actorA ! latch
      actorA ! Message(1, trace)
      latch.await()
      Tracer(system).endGroup("test")

      barrier("check-trace")

      val traces = Await.result(actorA ? GetTraces, timeout.duration).asInstanceOf[Set[UUID]]

      traces.size should be(1)
      trace foreach { _ should be(traces.head) }

      eventCheck("test")

      barrier("stop")

      // check remote system messages by stopping actorB

      Tracer(system).group("remote-stop") {
        system.stop(actorB)
      }

      eventCheck("stopped")
    }
  }
}
