/**
 * Copyright (C) 2009-2012 Typesafe Inc. <http://www.typesafe.com>
 */
package com.typesafe.trace.test

import akka.actor.ActorSystem
import akka.util.duration._
import com.typesafe.trace.{ DefaultActorSystemTracer, Tracer }
import com.typesafe.config.{ Config, ConfigFactory }
import java.util.concurrent.{ TimeUnit, TimeoutException }
import org.scalatest.matchers.MustMatchers
import org.scalatest.{ BeforeAndAfterAll, BeforeAndAfterEach }
import akka.util.Duration

object EchoTraceSpec {
  val config: Config = ConfigFactory.parseString("""
      akka {
        # TestEventHandler suppresses "simulated" errors
        event-handlers = ["com.typesafe.trace.test.TestEventHandler"]
        event-handler-startup-timeout = 10s
        loglevel = WARNING
        stdout-loglevel = WARNING
        actor {
          default-dispatcher {
            executor = "fork-join-executor"
            fork-join-executor {
              parallelism-min = 12
              parallelism-max = 12
            }
          }
        }
      }

      atmos {
        trace {
          enabled = true
          traceable {
            "*" = on
          }
          sampling {
            "*" = 1
          }
          futures = on
          events.futures = on
          use-dispatcher-monitor = off
          use-system-metrics-monitor = off
          buffer.size-limit = 0
        }

        test.time-factor = 1
      }""")
}

abstract class EchoTraceSpec(config: Config = EchoTraceSpec.config) extends CotestSyncSpec with MustMatchers with BeforeAndAfterAll with BeforeAndAfterEach {

  def this(conf: String) = this(ConfigFactory.parseString(conf).withFallback(EchoTraceSpec.config))

  val nodes = cotestNodes
  val nodeName = cotestName

  def cotestNodes = 2
  def cotestName = "trace"

  implicit var system: ActorSystem = _

  val timeoutHandler = TimeoutHandler(config.getInt("atmos.test.time-factor"))

  val timeFactor = config.getInt("atmos.test.time-factor")

  override def beforeAll(): Unit = {
    super.beforeAll()
    barrier("spec-create")
    createSystem()
    barrier("spec-start")
  }

  override def afterEach(): Unit = {
    barrier("spec-clear")
    barrier("spec-after")
    super.afterEach()
  }

  override def afterAll(): Unit = {
    barrier("spec-shutdown")
    shutdownSystem()
    barrier("spec-end")
    super.afterAll()
  }

  def createSystem(): Unit = {
    system = createSystem(config)
  }

  def createSystem(config: Config): ActorSystem = ActorSystem(testName, config)

  def testName(): String = getClass.getSimpleName.replaceAll("[^a-zA-Z0-9]", "")

  def eventCheck(name: String = "default"): Unit = {
    barrier(name + "-check-start")
    barrier(name + "-check-end", onSpin = flushTraceEvents)
  }

  def flushTraceEvents(): Unit = flushTraceEvents(system)

  def flushTraceEvents(system: ActorSystem): Unit = {
    getTracer(system).trace.local.flush()
  }

  def shutdownSystem(): Unit = {
    shutdownSystem(system)
    system = null
  }

  def shutdownSystem(system: ActorSystem): Unit = {
    if ((system ne null) && !system.isTerminated) {
      system.shutdown()
      try {
        getTracer(system).awaitShutdown(5 * timeFactor seconds)
      } catch {
        case _: TimeoutException ⇒ println("Failed to stop [%s] within expected time".format(system.name))
      }
    }
  }

  def getTracer(): DefaultActorSystemTracer = getTracer(system)

  def getTracer(system: ActorSystem): DefaultActorSystemTracer = {
    Tracer(system) match {
      case tracer: DefaultActorSystemTracer ⇒ tracer
      case _                                ⇒ throw new Exception("Not a traced actor system!")
    }
  }
}

case class TimeoutHandler(factor: Int) {
  private final val defaultTimeoutTime = 5000L

  def duration: Duration = Duration(defaultTimeoutTime * factor, TimeUnit.MILLISECONDS)

  def time: Long = defaultTimeoutTime * factor

  def unit: TimeUnit = TimeUnit.MILLISECONDS

  def timeoutify(originalDuration: Duration): Duration = originalDuration.*(factor)
}
