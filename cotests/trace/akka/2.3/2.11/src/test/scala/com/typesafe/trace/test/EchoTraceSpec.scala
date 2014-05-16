/**
 * Copyright (C) 2009-2012 Typesafe Inc. <http://www.typesafe.com>
 */
package com.typesafe.trace.test

import akka.actor.ActorSystem
import com.typesafe.trace.{ DefaultActorSystemTracer, ExecutionContextTracer, Tracer }
import com.typesafe.config.{ Config, ConfigFactory }
import java.util.concurrent.{ TimeUnit, TimeoutException }
import org.scalatest.matchers.MustMatchers
import org.scalatest.{ BeforeAndAfterAll, BeforeAndAfterEach }
import scala.concurrent.duration._
import scala.concurrent.duration.Duration
import scala.concurrent.duration.TimeUnit

object EchoTraceSpec {
  val config: Config = ConfigFactory.parseString("""
      akka {
        # TestLogger suppresses "simulated" errors
        loggers = ["com.typesafe.trace.test.TestLogger"]
        logger-startup-timeout = 10s
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

      activator {
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

        trace.test.time-factor = 1
      }""")
}

abstract class EchoTraceSpec(val config: Config = EchoTraceSpec.config) extends CotestSyncSpec with MustMatchers with BeforeAndAfterAll with BeforeAndAfterEach {

  def this(conf: String) = this(ConfigFactory.parseString(conf).withFallback(EchoTraceSpec.config))

  val nodes = cotestNodes
  val nodeName = cotestName

  def cotestNodes = 2
  def cotestName = "trace"

  var system: ActorSystem = _

  val timeoutHandler = TimeoutHandler(config.getInt("activator.trace.test.time-factor"))

  override val timeFactor = config.getInt("activator.trace.test.time-factor")

  override def beforeAll(): Unit = {
    System.setProperty("activator.trace.events.futures", "on")
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
    if (ExecutionContextTracer.global.enabled) ExecutionContextTracer.global.trace.local.flush()
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
        getTracer(system).awaitShutdown(timeoutHandler.duration)
      } catch {
        case _: TimeoutException ⇒ println("Failed to stop [%s] within expected period".format(system.name))
      }
    }
    if (ExecutionContextTracer.global.enabled) ExecutionContextTracer.global.trace.sender.shutdown()
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

  def timeoutify(originalDuration: Duration): FiniteDuration = originalDuration.*(factor).asInstanceOf[FiniteDuration]
}

