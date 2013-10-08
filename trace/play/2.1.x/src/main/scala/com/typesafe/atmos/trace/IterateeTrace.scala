/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */

package com.typesafe.atmos.trace

import com.typesafe.atmos.util.Uuid
import play.api.libs.iteratee.{ Iteratee, Step, Input }
import scala.concurrent.Future
import scala.util.{ Try, Success, Failure }

private[trace] object IterateeTrace {

  val ZeroIterateeInfo = IterateeInfo(Uuid.zero)

  def newInfo: IterateeInfo = IterateeInfo()

  def iterateeInfo(i: Iteratee[_, _]): Info = TraceInfo(i) match {
    case x: IterateeInfo ⇒ x
    case _               ⇒ NoInfo
  }

  class StepTracerHook[E, A, B](tracer: ActionTracer, info: IterateeInfo, folder: Step[E, A] ⇒ Future[B]) extends (Step[E, A] ⇒ Future[B]) {
    def apply(step: Step[E, A]): Future[B] = {
      val newStep: Step[E, A] = step match {
        case x: Step.Done[E, A] ⇒
          tracer.iteratee.done(info); x
        case Step.Cont(k)     ⇒ Step.Cont(new InputTracerHook(tracer, info, k))
        case x: Step.Error[E] ⇒ tracer.iteratee.error(info); x
      }
      folder(newStep)
    }
  }

  class InputTracerHook[E, A](tracer: ActionTracer, info: IterateeInfo, k: Input[E] ⇒ Iteratee[E, A]) extends (Input[E] ⇒ Iteratee[E, A]) {
    def apply(in: Input[E]): Iteratee[E, A] = {
      val next = k(in)
      iterateeInfo(next) match {
        case ii: IterateeInfo ⇒
          tracer.iteratee.continued(info, in match {
            case Input.El(v) ⇒ IterateeInput.El(tracer.trace.formatLongMessage(v))
            case Input.Empty ⇒ IterateeInput.Empty
            case Input.EOF   ⇒ IterateeInput.EOF
          }, ii)
        case _ ⇒
      }
      next
    }
  }
}

/**
 * Tracing events of Iteratees.
 */
private[trace] class IterateeTrace(val trace: Trace) extends WithTracing {
  import IterateeTrace._

  lazy val iterateeEvents = trace.settings.events.iteratees

  def enabled: Boolean = iterateeEvents && trace.tracing

  def created(info: IterateeInfo): Unit =
    withTE(IterateeCreated(info))

  def folded(info: IterateeInfo): Unit =
    withTE(IterateeFolded(info))

  def done(info: IterateeInfo): Unit =
    withTE(IterateeDone(info))

  def continued(info: IterateeInfo, input: IterateeInput.Tag, nextInfo: IterateeInfo): Unit =
    withTE(IterateeContinued(info, input, nextInfo))

  def error(info: IterateeInfo): Unit =
    withTE(IterateeError(info))

}
