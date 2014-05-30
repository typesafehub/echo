/**
 *  Copyright (C) 2011-2013 Typesafe <http://typesafe.com/>
 */
package com.typesafe.trace.store

import com.typesafe.trace._
import com.typesafe.trace.util.EchoSpec
import com.typesafe.trace.util.Uuid
import com.typesafe.trace.uuid.UUID
import org.scalatest.Matchers

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class MemoryTraceRepositorySpec extends EchoSpec with Matchers {

  "A MemoryTraceRepository" must {
    "store traces up to the limit" in {
      val repository = new MemoryTraceRepository(2)

      val trace1 = Uuid()
      val trace2 = Uuid()
      val trace3 = Uuid()

      def event(trace: UUID) = {
        TraceEvent(Marker("test", "1234"), system.name).copy(trace = trace)
      }

      val events = for {
        i ← (1 to 10)
        trace ← trace1 :: trace2 :: Nil
      } yield {
        event(trace)
      }
      repository.store(Batch(Seq(TraceEvents(events))))

      repository.trace(trace1).size should be(10)
      repository.trace(trace2).size should be(10)
      repository.allTraceIds.size should be(2)
      repository.allEventIds.size should be(20)

      val events3 = for (i ← (1 to 10)) yield event(trace3)
      repository.store(Batch(Seq(TraceEvents(events3))))

      repository.trace(trace1).size should be(0)
      repository.trace(trace2).size should be(10)
      repository.trace(trace3).size should be(10)
      repository.allTraceIds.size should be(2)
      repository.allEventIds.size should be(20)
    }
  }
}
