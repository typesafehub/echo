/*
  * Copyright Christopher Schmidt 2010
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.typesafe.trace.circuitbreaker

import com.typesafe.trace.circuitbreaker._
import com.typesafe.trace.circuitbreaker.CircuitBreaker._
import org.scalatest.{ Matchers, WordSpec }

/**
 * the TEST
 */

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class CircuitBreakerSpec extends WordSpec with Matchers {
  "A CircuitBreaker" must {

    /**
     * configure two CircuitBreaker
     */
    addCircuitBreaker("test", CircuitBreakerConfiguration(100, 10))
    addCircuitBreaker("test2", CircuitBreakerConfiguration(100, 10))

    "remain closed (no exceptions are thrown)" in {
      for (i ← 1 to 20) {
        new MyClass().myMethod()
      }
    }

    "be changing states correctly" in {

      /**
       * 10 failures throwing IllegalArgumentException
       */
      for (i ← 1 to 10) {
        an[java.lang.IllegalArgumentException] must be thrownBy { (new MyClass().my2Method) }
      }

      /**
       * calls are failing fast (invoke is not called) for
       * 100ms (this is configured for CircuitBreaker "test")
       */
      for (i ← 1 to 10) {
        an[CircuitBreakerOpenException] must be thrownBy { (new MyClass().my2Method) }
      }

      /**
       * sleep for more than 100ms
       */
      Thread.sleep(200)

      /**
       * CircuitBreaker should be half open then (after 100ms timeout)
       */
      an[CircuitBreakerHalfOpenException] must be thrownBy { (new MyClass().my2Method) }

      /**
       * still failing? then CircuitBreaker should be open again
       */
      for (i ← 1 to 10) {
        an[CircuitBreakerOpenException] must be thrownBy { (new MyClass().my2Method) }
      }
    }

  }
}

/**
 * class that uses trait UsingCircuitBreaker
 */
class MyClass extends UsingCircuitBreaker {

  /**
   * method that uses configured CircuitBreaker with name "test"
   * method works fine (means no exception)
   */
  def myMethod(): Unit = {
    withCircuitBreaker("test") {
    }
  }

  /**
   * method that uses configured CircuitBreaker with name "test"
   * method throws a IllegalArgumentException
   * means: Calls are failing
   */
  def my2Method: Long = {
    withCircuitBreaker("test") {
      throw new java.lang.IllegalArgumentException
    }
  }

}
