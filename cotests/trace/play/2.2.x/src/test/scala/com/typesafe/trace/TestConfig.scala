/**
 * Copyright (C) 2009-2012 Typesafe Inc. <http://www.typesafe.com>
 */
package com.typesafe.trace

import com.typesafe.config.{ Config, ConfigFactory }

object ActionTraceSpecConfig {
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
          futures = off
          use-dispatcher-monitor = off
          use-system-metrics-monitor = off
          buffer.size-limit = 0
        }

        trace.test.time-factor = 1
      }

      play {
        akka {
          loggers = ["com.typesafe.trace.test.TestLogger"]
          logger-startup-timeout = 10s
          loglevel = WARNING
          stdout-loglevel = WARNING
        }
      }""")
}
