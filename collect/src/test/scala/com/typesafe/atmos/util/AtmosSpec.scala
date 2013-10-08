/**
 * Copyright (C) 2011-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package com.typesafe.atmos.util

import akka.actor.ActorSystem
import akka.event.{ Logging, LoggingAdapter }
import akka.testkit.TestKit

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import java.util.concurrent.TimeoutException
import org.scalatest.matchers.MustMatchers
import org.scalatest.{ WordSpec, BeforeAndAfterAll }
import scala.concurrent.duration._
import java.util.concurrent.TimeUnit

object AtmosSpec {
  val testConf: Config = ConfigFactory.parseString("""
      akka {
        # TestEventHandler suppresses "simulated" errors
        event-handlers = ["com.typesafe.atmos.util.TestEventHandler"]
        loglevel = WARNING
        stdout-loglevel = WARNING
        event-handler-startup-timeout = 10s
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
          mongo {
            db-connection-uri = "mongodb://localhost"
            db-name = "test-atmos-monitoring"
          }
        }

        analytics {
          ignore-span-types = []
          ignore-span-time-series = []
          store-time-interval = 0
          percentiles-store-time-interval = 0
          store-limit = 30000
          store-use-random-interval = false
          store-use-all-time = true
          store-use-duplicates-cache = true
          store-flush-delay = 2
          actor-path-time-ranges = ["minutes", "hours", "days", "months", "all"]

          save-spans = on
          mongo {
            db-connection-uri = "mongodb://localhost"
            db-name = "test-atmos-monitoring"
          }
        }
        trace-tree {
          mongo {
            db-connection-uri = "mongodb://localhost"
            db-name = "test-atmos-trace-tree"
          }
        }
        subscribe.notification-event-log-size = 500000

        test.time-factor = 1
      }""")

  def getCallerName: String = {
    val s = Thread.currentThread.getStackTrace map (_.getClassName) drop 1 dropWhile (_ matches ".*AtmosSpec.?$")
    s.head.replaceFirst(""".*\.""", "").replaceAll("[^a-zA-Z_0-9]", "_")
  }

}

abstract class AtmosSpec(_system: ActorSystem) extends TestKit(_system) with WordSpec with MustMatchers with BeforeAndAfterAll {

  def this(config: Config) =
    this(ActorSystem(AtmosSpec.getCallerName,
      ConfigFactory.defaultOverrides
        .withFallback(config)
        .withFallback(AtmosSpec.testConf)
        .withFallback(ConfigFactory.defaultReference)))

  def this(conf: String) = this(ConfigFactory.parseString(conf))

  def this() = this(ActorSystem(AtmosSpec.getCallerName, AtmosSpec.testConf))

  val log: LoggingAdapter = Logging(system, this.getClass)

  def config: Config = system.settings.config

  def timeoutHandler = TimeoutHandler(config.getInt("atmos.test.time-factor"))

  val timeFactor = timeoutHandler.factor

  override def beforeAll(): Unit = {
    atStartup()
  }

  override def afterAll(): Unit = {
    shutdownSystem()
    atTermination()
  }

  def shutdownSystem(): Unit = {
    system.shutdown()
    try {
      system.awaitTermination(timeoutHandler.duration)
    } catch {
      case _: TimeoutException ⇒ println("Failed to stop [%s] within expected shutdown time".format(system.name))
    }
  }

  protected def atStartup(): Unit = {}

  protected def atTermination(): Unit = {}
}

case class TimeoutHandler(factor: Int) {
  import java.util.concurrent.{ CountDownLatch, TimeUnit }
  private final val defaultTimeoutTime = 5000L

  def duration: FiniteDuration = Duration(defaultTimeoutTime * factor, TimeUnit.MILLISECONDS)

  def time: Long = defaultTimeoutTime * factor

  def unit: TimeUnit = TimeUnit.MILLISECONDS

  def timeoutify(originalDuration: Duration): Duration = originalDuration.*(factor)

  def awaitLatch(latch: CountDownLatch, timeout: Long, unit: TimeUnit): Boolean =
    latch.await(timeout * factor, unit)
}
