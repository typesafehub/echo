/**
 * Copyright (C) 2009-2012 Typesafe Inc. <http://www.typesafe.com>
 */
package com.typesafe.trace

import com.typesafe.config.Config
import play.api.Play

abstract class ActionTraceSpec(config: Config = ActionTraceSpecConfig.config) extends ActionTraceSpecBase(config) {
  override def beforeAllInit(): Unit = {
    Play.start(app)
  }

  override def afterAllCleanup(): Unit = {
    Play.stop()
  }
}
