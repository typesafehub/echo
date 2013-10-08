/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */

package com.typesafe.trace

import akka.actor.{ Actor, ActorRef, ActorSystem, Props }
import akka.pattern.gracefulStop
import com.typesafe.trace.util.OnDemand
import com.typesafe.config.ConfigFactory
import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.Await
import scala.concurrent.duration._

/**
 * Wrapper for creating atmos actors in a separate untraceable actor system.
 */
object AtmosSystem {
  val StopTimeout = 10.seconds

  private val counter = new AtomicInteger(0)

  private val config = {
    import scala.collection.JavaConverters._
    val appConfig = ConfigFactory.load(akka.trace.Private.findClassLoader())
    val internalSystemConfig = appConfig.getConfig("atmos.trace.internal-system")
    // use the loglevel and daemonic settings from the appConfig as default
    val defaultConfigMap = Map(
      "akka.loglevel" -> appConfig.getString("akka.loglevel"),
      "akka.daemonic" -> appConfig.getBoolean("akka.daemonic"),
      "akka.actor.serialize-creators" -> false)

    val config =
      ConfigFactory.parseString("atmos.trace.enabled = false").
        withFallback(ConfigFactory.defaultOverrides).
        withFallback(internalSystemConfig).
        withFallback(ConfigFactory.parseMap(defaultConfigMap.asJava)).
        withFallback(ConfigFactory.defaultReference)
    config.checkValid(ConfigFactory.defaultReference, "atmos")
    config
  }

  private val actorSystem = new OnDemand[ActorSystem] {
    def create(): ActorSystem = ActorSystem("atmos", config)
    def shutdown(system: ActorSystem) = system.shutdown
  }

  def enabled[A](f: ActorSystem ⇒ A): A = {
    val system = actorSystem.access()
    try { f(system) } finally { actorSystem.release() }
  }

  def create(props: Props, name: String): ActorRef = {
    actorSystem.access().actorOf(props, named(name))
  }

  private def named(name: String): String = {
    counter.incrementAndGet.toString + "_" + name
  }

  def stop(actor: ActorRef): Unit = {
    actorSystem { system ⇒
      try {
        Await.ready(gracefulStop(actor, StopTimeout)(system), StopTimeout)
      } catch {
        case e: Exception ⇒ system.log.error(e, "Failed to gracefully stop atmos actor [" + actor.path + "]")
      }
    }
    actorSystem.release()
  }
}
