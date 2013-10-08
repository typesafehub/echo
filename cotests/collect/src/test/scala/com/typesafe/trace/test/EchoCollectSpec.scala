/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */

package com.typesafe.trace.test

import com.typesafe.trace._
import com.typesafe.trace.store.{ MemoryTraceEventListener, MemoryTraceRepository }
import com.typesafe.trace.util.EchoSpec
import com.typesafe.config.{ Config, ConfigFactory }
import org.scalatest.BeforeAndAfterEach
import java.util.concurrent.TimeUnit
import scala.concurrent.duration._
import scala.reflect.ClassTag

object EchoCollectSpec {
  val config: Config = ConfigFactory.parseString("")
}

abstract class EchoCollectSpec(_config: Config = EchoCollectSpec.config) extends EchoSpec(_config) with CotestSyncSpec with BeforeAndAfterEach {
  def this(conf: String) = this(ConfigFactory.parseString(conf).withFallback(EchoCollectSpec.config))

  def testFactor = _config.getInt("atmos.test.time-factor")

  val defaultTimeout = 5.seconds
  val waitSleep = 20.millis
  val waitTimeout = timeoutHandler.timeoutify(defaultTimeout)

  val nodes = cotestNodes
  val nodeName = cotestName

  def cotestNodes = 2
  def cotestName = "collect"

  val traceReceiver = TraceReceiver(config)

  lazy val repository: MemoryTraceRepository = {
    MemoryTraceEventListener.getRepositoryFor(traceReceiver).getOrElse {
      throw new Exception("Need to use MemoryTraceEventListener with EchoCollectSpec")
    }
  }

  override def beforeAll(): Unit = {
    super.beforeAll()
    barrier("spec-create")
    barrier("spec-start")
  }

  override def afterEach(): Unit = {
    barrier("spec-clear")
    clearEvents()
    barrier("spec-after")
    super.afterEach()
  }

  override def afterAll(): Unit = {
    barrier("spec-shutdown")
    barrier("spec-end")
    traceReceiver.shutdown()
    clearEvents()
    super.afterAll()
  }

  def eventCheck(name: String = "default", expected: Int = 0, timeout: Duration = defaultTimeout, delay: Duration = 0.millis, printAfterWait: Boolean = false, printOnError: Boolean = false)(checkEventTypes: ⇒ Unit): Unit = {
    barrier(name + "-check-start")
    waitForEvents(expected, timeout = timeoutHandler.timeoutify(timeout), printAfterWait = printAfterWait)
    Thread.sleep(delay.toMillis)
    if (printOnError) {
      try { countEvents must be(expected) }
      catch {
        case e: Exception ⇒
          printTraces
          throw e
      }
    } else countEvents must be(expected)
    checkEventTypes
    clearEvents()
    barrier(name + "-check-end")
  }

  def eventOneOfCheck[T](expectedOptions: Map[Int, T], name: String = "default", timeout: Duration = defaultTimeout, delay: Duration = 0.millis, printAfterWait: Boolean = false, printOnError: Boolean = false)(checkEventTypes: T ⇒ Unit): Unit = {
    barrier(name + "-check-start")
    val options = expectedOptions.keySet
    waitForOneOfEvents(options, timeout = timeoutHandler.timeoutify(timeout), printAfterWait = printAfterWait)
    Thread.sleep(delay.toMillis)
    if (printOnError) {
      try { options(countEvents) must be(true) }
      catch {
        case e: Exception ⇒
          printTraces
          throw e
      }
    } else options(countEvents) must be(true)
    checkEventTypes(expectedOptions(countEvents))
    clearEvents()
    barrier(name + "-check-end")
  }

  def clearEvents(): Unit = if (repository != null) repository.clear()

  // override this and return true in test to printTraces after waitForEvents
  def printTracesAfterWaitForEvents: Boolean = false

  def includeSystemStartedEvents: Boolean = false

  def includeRemoteStatusEvents: Boolean = false

  def waitForTraces(n: Int, sleep: Duration = waitSleep, timeout: Duration = waitTimeout): Unit = {
    def test = { countTraces >= n }
    if (!waitFor(test, sleep, timeout))
      fail("Timeout after %s waiting for %s traces. Only %s traces.".format(timeout, n, countTraces))
  }

  def waitForEvents(n: Int, sleep: Duration = waitSleep, timeout: Duration = waitTimeout, printAfterWait: Boolean = false): Unit = {
    def test = { countEvents >= n }
    try {
      if (!waitFor(test, sleep, timeout))
        fail("Timeout after %s waiting for %s events. Only %s events.".format(timeout, n, countEvents))
    } finally {
      if (printAfterWait || printTracesAfterWaitForEvents) printTraces()
    }
  }

  def waitForOneOfEvents(ns: Set[Int], sleep: Duration = waitSleep, timeout: Duration = waitTimeout, printAfterWait: Boolean = false): Unit = {
    def test = { ns(countEvents) }
    try {
      if (!waitFor(test, sleep, timeout))
        fail("Timeout after %s waiting for one of %s events. Only %s events.".format(timeout, ns, countEvents))
    } finally {
      if (printAfterWait || printTracesAfterWaitForEvents) printTraces()
    }
  }

  // wait for events, check count, and then clear events
  def discardEvents(n: Int, sleep: Duration = waitSleep, timeout: Duration = waitTimeout): Unit = {
    waitForEvents(n, sleep, timeout)
    Thread.sleep(sleep.toMillis) // extra sleep to allow for events
    val count = countEvents
    if (count != n) fail("Expected %s events but there are %s events" format (n, count))
    clearEvents()
  }

  def waitFor(test: ⇒ Boolean, sleep: Duration = waitSleep, timeout: Duration = waitTimeout): Boolean = {
    val start = System.currentTimeMillis
    val limit = start + timeout.toMillis
    var passed = test
    var expired = false
    while (!passed && !expired) {
      if (System.currentTimeMillis > limit) expired = true
      else {
        Thread.sleep(sleep.toMillis)
        passed = test
      }
    }
    passed
  }

  def allEvents: Iterable[TraceEvent] = {
    if (repository == null) Nil
    else repository.allEvents.filter(_.annotation match {
      case x: SystemStarted     ⇒ includeSystemStartedEvents
      case x: RemoteStatus      ⇒ includeRemoteStatusEvents
      case x: RemotingLifecycle ⇒ includeRemoteStatusEvents
      case _                    ⇒ true
    })
  }

  def allTraces: Iterable[Set[TraceEvent]] = allEvents.groupBy(_.trace).values.map(_.toSet)

  def orderedTraces = allTraces.toSeq.sortBy(t ⇒ t.map(_.nanoTime).min)

  def countTraces = allTraces.size

  def orderedEvents = allEvents.toSeq.sortBy(_.nanoTime)

  def countEvents = allEvents.size

  def groupedEvents: Map[Class[_], List[TraceEvent]] = allEvents.toList.groupBy(_.annotation.getClass)

  def eventsOf[A](implicit m: ClassTag[A]) = groupedEvents.getOrElse(m.runtimeClass, Nil)

  def annotationsOf[A](implicit m: ClassTag[A]) = eventsOf[A].map(_.annotation).asInstanceOf[List[A]]

  def countEventsOf[A](implicit m: ClassTag[A]) = groupedEvents.getOrElse(m.runtimeClass, Nil).size

  // use this when the annotation is a case object
  def countEventsOf(annotation: Annotation) = countEvents(_.annotation == annotation)

  def countEvents(filter: TraceEvent ⇒ Boolean) = allEvents.filter(filter).size

  def countSysMsgEventsOf[E, M](implicit em: ClassTag[E], mm: ClassTag[M]) = {
    groupedEvents.getOrElse(em.runtimeClass, Nil).filter(_.annotation match {
      case sysMsg: SysMsgAnnotation ⇒ sysMsg.message.getClass == mm.runtimeClass
      case _                        ⇒ false
    }).size
  }

  // use this when the system message is a case object
  def countSysMsgEventsOf[E](message: SysMsg)(implicit em: ClassTag[E]) = {
    groupedEvents.getOrElse(em.runtimeClass, Nil).filter(_.annotation match {
      case sysMsg: SysMsgAnnotation ⇒ sysMsg.message == message
      case _                        ⇒ false
    }).size
  }

  def printTraces() = orderedTraces.map(PrettyTrace.show).foreach(println)

  def printEvents() = orderedEvents.foreach(e ⇒ { println(e); println })

  def printAnnotations() = orderedEvents.map(_.annotation).foreach(println)
}
