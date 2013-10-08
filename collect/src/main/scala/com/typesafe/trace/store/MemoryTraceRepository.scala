/**
 *  Copyright (C) 2011-2013 Typesafe <http://typesafe.com/>
 */
package com.typesafe.trace
package store

import akka.actor.ActorRef
import com.typesafe.trace.uuid.UUID
import com.typesafe.config.Config
import java.util.concurrent.CountDownLatch
import scala.collection.mutable

class MemoryTraceRepository(limit: Int = 10000) extends TraceStorageRepository with TraceRetrievalRepository {

  private var traces = emptyTraceMap
  private var traceOrder = emptyQueue
  private var events = emptyEventMap
  private var latch: Option[CountDownLatch] = None
  private var latchCondition: ((TraceEvents) ⇒ Boolean) = _

  def store(batch: Batch): Unit = synchronized {
    for (traceEvents ← batch.payload) {
      traceEvents.events.foreach(store)
      for (cdl ← latch if latchCondition(traceEvents)) cdl.countDown
    }
  }

  private def store(event: TraceEvent): Unit = {
    val trace = event.trace
    if (!traces.isDefinedAt(trace)) {
      traces(trace) = emptySet
      traceOrder += trace
    }
    traces(trace) += event
    events(event.id) = event
    if (traceOrder.size > limit) {
      val id = traceOrder.dequeue
      for (event ← traces(id)) events -= event.id
      traces -= id
    }
  }

  def trace(id: UUID): Seq[TraceEvent] = synchronized {
    val result = traces.get(id).map(_.toSeq).getOrElse(Seq())
    result.sortBy(_.nanoTime)
  }

  def event(id: UUID): Option[TraceEvent] = synchronized {
    events.get(id)
  }

  def events(ids: Seq[UUID]): Seq[TraceEvent] = synchronized {
    val found = ids.map(event(_)).flatten
    sortByTime(found)
  }

  def findEventsWithinTimePeriod(startTime: Long, endTime: Long, offset: Int, limit: Int): Seq[TraceEvent] = synchronized {
    def isInTimeRange(timestamp: Long): Boolean = {
      timestamp >= startTime && timestamp <= endTime
    }

    val found = events.values.filter(x ⇒ isInTimeRange(x.timestamp)).toSeq
    val sorted = sortByTime(found)
    val page = sorted.slice(offset - 1, offset - 1 + limit)
    page
  }

  private def sortByTime(events: Seq[TraceEvent]) = {
    val sorted = events.sortWith { (a, b) ⇒
      if (a.timestamp == b.timestamp)
        a.nanoTime < b.nanoTime
      else
        a.timestamp < b.timestamp
    }
    sorted
  }

  def allTraceIds: Iterable[UUID] = synchronized {
    traces.keys.toList
  }

  def allEventIds: Iterable[UUID] = synchronized {
    events.keys.toList
  }

  def allTraces: Iterable[Set[TraceEvent]] = synchronized {
    traces.values.map(_.toSet).toList
  }

  def allEvents: Iterable[TraceEvent] = synchronized {
    events.values.toList
  }

  def countTraces: Int = synchronized {
    traces.size
  }

  def countEvents: Int = synchronized {
    events.size
  }

  def clear(): Unit = synchronized {
    traces = emptyTraceMap
    events = emptyEventMap
    traceOrder = emptyQueue
  }

  /**
   * For test purpose. The latch will be counted down for each TraceEvents
   * that is stored 'when' the condition is fulfilled.
   */
  def useLatch(cdl: CountDownLatch)(when: (TraceEvents) ⇒ Boolean): Unit = synchronized {
    latch = Some(cdl)
    latchCondition = when
  }

  private def emptyTraceMap = mutable.Map[UUID, mutable.Set[TraceEvent]]()

  private def emptyEventMap = mutable.Map[UUID, TraceEvent]()

  private def emptySet = mutable.Set[TraceEvent]()

  private def emptyQueue = mutable.Queue[UUID]()
}

/**
 * Global singleton repository.
 */
object LocalMemoryTraceRepository extends MemoryTraceRepository

/**
 * Store memory repository and store for memory event listeners.
 */
object MemoryTraceEventListener {

  private[this] var storage: Map[String, MemoryTraceRepository] = Map.empty

  private def get(id: String): MemoryTraceRepository = synchronized {
    storage.get(id).getOrElse {
      val created = if (id == "local") LocalMemoryTraceRepository else new MemoryTraceRepository
      storage += id -> created
      created
    }
  }

  def getRepository(id: String): MemoryTraceRepository = get(id)

  def getRepositoryFor(receiver: TraceReceiver): Option[MemoryTraceRepository] = {
    getIdFor(receiver) map getRepository
  }

  def getIdFor(receiver: TraceReceiver): Option[String] = {
    if (useLocal(receiver)) Some("local") else getListenerFor(receiver) map { _.path.name }
  }

  def useLocal(receiver: TraceReceiver): Boolean = useLocal(receiver.eventHandler.config)

  def useLocal(config: Config): Boolean = config.getString("atmos.mode") == "local"

  def getListenerFor(receiver: TraceReceiver): Option[ActorRef] = {
    receiver.eventHandler.listeners.find { _.path.name contains "MemoryTraceEventListener" }
  }

  def remove(id: String): Unit = synchronized {
    storage -= id
  }
}

/**
 * Event listener that publishes to in-memory store.
 */
class MemoryTraceEventListener(override val config: Config) extends TraceEventListener {
  import MemoryTraceEventListener.{ getRepository, remove }

  val useLocal = MemoryTraceEventListener.useLocal(config)

  override def repository: MemoryTraceRepository =
    if (useLocal) getRepository("local") else getRepository(self.path.name)

  override def postStop = remove(self.path.name)
}
