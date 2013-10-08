/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */
package com.typesafe.atmos.trace

import com.typesafe.atmos.util.Uuid
import com.typesafe.atmos.uuid.UUID
import scala.annotation.tailrec

case class TraceTree(root: Option[TraceTree.Node])

object TraceTree {

  private val MaxDepth = 1000

  case class Node(event: TraceEvent, children: Seq[Node])

  private class NodeBuilder(val event: TraceEvent, var children: Seq[NodeBuilder] = Nil) {
    def toNode: Node = {
      def build(b: NodeBuilder, depth: Int): Node = {
        if (depth > MaxDepth) Node(b.event, Nil)
        else Node(b.event, b.children.map(build(_, depth + 1)))
      }
      build(this, 1)
    }
  }

  def apply(trace: Seq[TraceEvent]): TraceTree = {

    val allEvents = (trace map { n ⇒ (n.id, n) }).toMap

    @tailrec
    def startOfLocalSpan(event: TraceEvent): Option[TraceEvent] = {
      if (Uuid.isZero(event.parent)) None
      else {
        allEvents.get(event.parent) match {
          case None ⇒ None
          case x @ Some(parent) if parent.local != event.local ⇒ x
          case Some(parent) ⇒ startOfLocalSpan(parent)
        }
      }
    }

    val nodeBuilders = (trace map { n ⇒ (n.id, new NodeBuilder(n)) }).toMap

    val locals = trace groupBy (_.local)
    val sortedLocals = locals.values.toSeq.map(_.toSeq.sortBy(_.nanoTime)).sortBy(_.head.nanoTime)

    for (events ← sortedLocals) {
      val start = startOfLocalSpan(events.head)
      for (s ← start) {
        nodeBuilders(s.id).children ++= events.map(e ⇒ nodeBuilders(e.id))
      }
    }

    val root = trace find { n ⇒ Uuid.isZero(n.parent) }

    val rootNode = root.map(e ⇒ nodeBuilders(e.id).toNode)
    TraceTree(rootNode)

  }

}
