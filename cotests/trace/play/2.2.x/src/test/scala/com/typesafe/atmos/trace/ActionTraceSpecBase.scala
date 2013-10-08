/**
 * Copyright (C) 2009-2012 Typesafe Inc. <http://www.typesafe.com>
 */
package com.typesafe.atmos.trace

import com.typesafe.config.{ Config, ConfigFactory }
import org.scalatest.matchers.MustMatchers
import org.scalatest.{ BeforeAndAfterAll, BeforeAndAfterEach }
import play.api.test.FakeApplication
import play.api.Play
import com.typesafe.atmos.test.CotestSyncSpec

abstract class ActionTraceSpecBase(val config: Config = ActionTraceSpecConfig.config) extends CotestSyncSpec with MustMatchers with BeforeAndAfterAll with BeforeAndAfterEach {

  def this(conf: String) = this(ConfigFactory.parseString(conf).withFallback(ActionTraceSpecConfig.config))

  val nodes = cotestNodes
  val nodeName = cotestName

  def cotestNodes = 2
  def cotestName = "trace"

  var app: FakeApplication = _

  override val timeFactor = config.getInt("atmos.test.time-factor")

  def beforeAllInit(): Unit = ()
  def afterEachStep(): Unit = ()
  def afterAllCleanup(): Unit = ()

  override def beforeAll(): Unit = {
    super.beforeAll()
    barrier("spec-create")
    app = TestApplication.application
    beforeAllInit()
    val ec = play.api.libs.concurrent.Execution.defaultContext // initialize object
    val ec1 = ExecutionInitializer.internalContext // initialize object
    barrier("spec-start")
  }

  override def afterEach(): Unit = {
    barrier("spec-clear")
    afterEachStep()
    barrier("spec-after")
    super.afterEach()
  }

  override def afterAll(): Unit = {
    barrier("spec-shutdown")
    afterAllCleanup()
    barrier("spec-end")
    super.afterAll()
  }

  def eventCheck(name: String = "default"): Unit = {
    barrier(name + "-check-start")
    barrier(name + "-check-end", onSpin = flushTraceEvents)
  }

  def flushTraceEvents(): Unit = {
    if (ActionTracer.global.enabled) ActionTracer.global.trace.local.flush()
    if (ExecutionContextTracer.global.enabled) ExecutionContextTracer.global.trace.local.flush()
  }

}
