/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */

package com.typesafe.atmos.trace

import com.typesafe.atmos.util.Uuid
import play.api.libs.iteratee.{ Iteratee, Step, Input }
import scala.concurrent.Future
import scala.util.{ Try, Success, Failure }

class IterateeTagData(var chunking: Boolean = false)

trait IterateeTag {
  def tag: IterateeTagData
}

private[trace] object IterateeTrace {

  val ZeroIterateeInfo = IterateeInfo(Uuid.zero)

  val CR: Byte = "\r".getBytes("UTF-8")(0)

  val ZeroIterateeTagData = new IterateeTagData()
  val Chunking = new IterateeTagData(true)

  def newInfo: IterateeInfo = IterateeInfo()

  def iterateeInfo(i: Iteratee[_, _]): Info = TraceInfo(i) match {
    case x: IterateeInfo ⇒ x
    case _               ⇒ NoInfo
  }

  def iterateeTag(i: Iteratee[_, _]): IterateeTag = i match {
    case x: IterateeTag ⇒ x
    case _              ⇒ null
  }

  class StepTracerHook[E, A, B](tracer: ActionTracer, info: IterateeInfo, chunking: Boolean, folder: Step[E, A] ⇒ Future[B]) extends (Step[E, A] ⇒ Future[B]) {
    def apply(step: Step[E, A]): Future[B] = {
      val newStep: Step[E, A] = step match {
        case x: Step.Done[E, A] ⇒
          tracer.iteratee.done(info); x
        case Step.Cont(k)     ⇒ Step.Cont(new InputTracerHook(tracer, info, chunking, k))
        case x: Step.Error[E] ⇒ tracer.iteratee.error(info); x
      }
      folder(newStep)
    }
  }

  class InputTracerHook[E, A](tracer: ActionTracer, info: IterateeInfo, chunking: Boolean, k: Input[E] ⇒ Iteratee[E, A]) extends (Input[E] ⇒ Iteratee[E, A]) {
    def apply(in: Input[E]): Iteratee[E, A] = {
      val next = k(in)
      if (chunking) {
        val it = iterateeTag(next)
        if (it != null) it.tag.chunking = true
      }
      iterateeInfo(next) match {
        case ii: IterateeInfo ⇒
          tracer.iteratee.continued(info, in match {
            case Input.El(v) ⇒
              if (chunking) {
                val a = v.asInstanceOf[Array[Byte]]
                val overhead = a.indexOf(CR) + 4
                tracer.netty.writeChunk(overhead, a.length - overhead)
              }
              IterateeInput.El(tracer.trace.formatLongMessage(v))
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
