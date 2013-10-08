/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */

package com.typesafe.atmos.trace

import com.typesafe.atmos.util.Uuid
import com.typesafe.atmos.uuid.UUID
import scala.annotation.tailrec

/**
 * Displaying traces.
 */
object PrettyTrace {
  def show(trace: Set[TraceEvent]): String = {
    val traceName = "---- trace: " + trace.headOption.map(_.trace.toString).getOrElse("") + " ----"
    val nodes = (trace map { n ⇒ (n.id, n) }).toMap
    val root = trace find { n ⇒ Uuid.isZero(n.parent) }
    val locals = trace groupBy (_.local)
    val spans = locals mapValues { events ⇒ events.toList.sortBy(_.nanoTime) }
    val children = trace groupBy (_.parent) filterNot { case (id, events) ⇒ Uuid.isZero(id) }
    val branches = children map { case (id, events) ⇒ (id, events.filter(e ⇒ nodes.isDefinedAt(id) && e.local != nodes(id).local)) }
    val branching = branches filter { case (id, events) ⇒ events.size > 0 }
    val dangling = (children.values.flatten.filter { e ⇒ !nodes.isDefinedAt(e.parent) }).toList

    @tailrec
    def order(search: List[UUID], ordered: List[UUID]): List[UUID] = {
      if (search.isEmpty) ordered.reverse
      else {
        val current = search.head
        val span = spans(current)
        val branched = span flatMap { event ⇒ branching.get(event.id) }
        val nextLocals = branched flatMap { b ⇒ b.map(_.local) }
        order(search.tail ::: nextLocals, current :: ordered)
      }
    }

    def showFromRoot(root: TraceEvent): String = {
      val ordered = order(List(root.local), Nil)
      val indices = ordered.zipWithIndex.toMap
      def showSpan(id: UUID): List[String] = {
        val span = spans(id)
        val eventStrings = span map { event ⇒
          val branched = branching.get(event.id) map { events ⇒ events.map(_.local) } getOrElse Set.empty[UUID]
          val numbers = branched.map(indices)
          val branchString = if (numbers.isEmpty) "" else numbers.mkString(" -> ", " -> ", "")
          val eventString = event.annotation.toString
          eventString + branchString
        }
        "" :: (indices(id) + ":") :: eventStrings
      }
      ordered flatMap showSpan mkString "\n"
    }

    val rootTrace = (root map showFromRoot orElse Some("\n-- no root trace --")).toList
    val danglingTraces = dangling map showFromRoot map ("-- partial trace --\n" + _)

    traceName + "\n" + (rootTrace ++ danglingTraces).mkString("", "\n\n", "\n")
  }
}
