/**
 * Copyright (C) 2009-2012 Typesafe Inc. <http://www.typesafe.com>
 */
package com.typesafe.atmos.trace

import com.typesafe.config.Config
import play.api.test.TestServer

abstract class ActionTraceNettySpec(config: Config = ActionTraceSpecConfig.config) extends ActionTraceSpecBase(config) {

  var server: TestServer = _

  override def beforeAllInit(): Unit = {
    server = TestServer(9876, app)
    server.start()
  }

  override def afterAllCleanup(): Unit = {
    server.stop()
  }

}

