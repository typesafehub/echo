/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */
package com.typesafe.atmos.trace

import akka.actor.Actor
import akka.event.Logging._
import akka.event.slf4j.Logger
import akka.event.slf4j.Slf4jEventHandler
import org.slf4j.MDC

class Slf4jTraceContextEventHandler extends Slf4jEventHandler {
  import Slf4jTraceContextEventHandler._

  def addContext: PartialFunction[Any, Any] = {
    case logEvent ⇒
      val current = Tracer(context.system) match {
        case tracer: ActorSystemTracer if tracer.trace ne null ⇒ tracer.trace.local.current
        case _ ⇒ None
      }
      for (x ← current) MDC.put(TraceId, x.context.trace.toString)
      logEvent
  }

  override def receive = {
    addContext.andThen(superReceive)
  }

  // Backport of Akka issue: http://www.assembla.com/spaces/akka/tickets/2002
  def superReceive: Actor.Receive = {

    case event @ Error(cause, logSource, logClass, message) ⇒
      withMdc(logSource, event.thread.getName) {
        cause match {
          case Error.NoCause | null ⇒ Logger(logClass, logSource).error(if (message != null) message.toString else null)
          case cause                ⇒ Logger(logClass, logSource).error(if (message != null) message.toString else cause.getLocalizedMessage, cause)
        }
      }

    case event @ Warning(logSource, logClass, message) ⇒
      withMdc(logSource, event.thread.getName) { Logger(logClass, logSource).warn("{}", message.asInstanceOf[AnyRef]) }

    case event @ Info(logSource, logClass, message) ⇒
      withMdc(logSource, event.thread.getName) { Logger(logClass, logSource).info("{}", message.asInstanceOf[AnyRef]) }

    case event @ Debug(logSource, logClass, message) ⇒
      withMdc(logSource, event.thread.getName) { Logger(logClass, logSource).debug("{}", message.asInstanceOf[AnyRef]) }

    case InitializeLogger(_) ⇒
      log.info("Slf4jEventHandler started")
      sender ! LoggerInitialized
  }
}

object Slf4jTraceContextEventHandler {
  val TraceId = "traceId"
}
