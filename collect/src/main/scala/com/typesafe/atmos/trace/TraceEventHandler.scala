/**
 *  Copyright (C) 2011-2013 Typesafe <http://typesafe.com/>
 */
package com.typesafe.atmos.trace

import akka.actor._
import com.typesafe.atmos.circuitbreaker._
import com.typesafe.atmos.trace.store.TraceStorageRepository
import com.typesafe.config.Config
import scala.collection.JavaConverters._
import scala.util.{ Failure, Success }

object TraceEventHandler {
  val DispatcherId = "atmos-trace-dispatcher"

  def apply(config: Config) = new TraceEventHandler(config)
}

class TraceEventHandler(val config: Config) {

  val listenerNames = config.getStringList("atmos.trace.event-handlers").asScala

  val listeners: Seq[ActorRef] = createListeners

  def publish(batch: Batch) {
    listeners foreach { listener ⇒
      try {
        listener ! batch
      } catch {
        case e: Exception ⇒ // drop batch
      }
    }
  }

  def shutdown(): Unit = {
    AtmosActor.enabled { system ⇒
      listeners foreach AtmosActor.stop
      system.log.debug("Stopping trace event handlers")
    }
  }

  def createListeners: Seq[ActorRef] = {

    AtmosActor.enabled { system ⇒
      system.log.debug("Starting trace event handlers")
      listenerNames flatMap { name ⇒
        try {
          Some(createListenerActor(name))
        } catch {
          case e: Exception ⇒
            system.log.error(e, "Trace listener [{}] cannot be loaded, due to [{}]", name, e.getMessage)
            None
        }
      }
    }
  }

  def createListenerActor(fqcn: String): ActorRef = {
    val dynamicAccess = new ReflectiveDynamicAccess(getClass.getClassLoader)
    val constructorParams = Seq((classOf[Config], config))
    val props = Props(
      dynamicAccess.createInstanceFor[Actor](fqcn, constructorParams) match {
        case Success(listenerClass) ⇒ listenerClass
        case Failure(exception)     ⇒ throw exception
      }).withDispatcher(TraceEventHandler.DispatcherId)
    AtmosActor.create(props, fqcn)
  }
}

abstract class TraceEventListener extends Actor with UsingCircuitBreaker with ActorLogging {
  private var retryBuffer = Vector[Batch]()

  /**
   *  Subclass must specify repository implementation
   */
  def repository: TraceStorageRepository

  /**
   * Subclass should take config as constructor parameter
   */
  val config: Config

  val circuitBreakerName = {
    val circuitBreakerConfiguration = {
      val timeoutMillis = config.getMilliseconds("atmos.trace.storage-circuit-breaker.timeout")
      val failureThreshold = config.getInt("atmos.trace.storage-circuit-breaker.failure-threshold")
      CircuitBreakerConfiguration(timeoutMillis, failureThreshold)
    }

    val circuitBreakerName = "StorageCircuitBreaker"
    if (!CircuitBreaker.hasCircuitBreaker(circuitBreakerName))
      CircuitBreaker.addCircuitBreaker(circuitBreakerName, circuitBreakerConfiguration)
    circuitBreakerName
  }

  /**
   * Number of batches that will be kept in memory later retry in case of failure sending to storage.
   */
  private val retryBufferSize: Int = config.getInt("atmos.trace.storage-retry-buffer-size")

  def receive = {
    case batch: Batch ⇒ handle(batch)
  }

  def handle(batch: Batch) = {
    try withCircuitBreaker(circuitBreakerName) {
      if (!retryBuffer.isEmpty) {
        retryStore()
      }

      store(batch)
    } catch {
      case e: CircuitBreakerOpenException ⇒
        // circuit breaker is open - wait until timeout is fulfilled
        addToRetryBuffer(batch)
        log.debug("Trace event handler not available to store batch, reason: [%s]" format e.getMessage)
      case e: CircuitBreakerHalfOpenException ⇒
        // timeout time is fulfilled, try one time to check if okay and either open or close circuit breaker depending on outcome
        // don't propagate exception, since this isn't used with supervisor, or needs to be restarted
        addToRetryBuffer(batch)
        log.error(e, "Trace event handler failing to store batch, reason: [%s]" format e.getMessage)
      case e: Exception ⇒
        // throw exception and increment threshold count in circuit breaker
        // don't propagate exception, since this isn't used with supervisor, or needs to be restarted
        addToRetryBuffer(batch)
        log.error(e, "Trace event handler failed to store batch, reason: [%s]" format e.getMessage)
    }
  }

  protected def store(batch: Batch): Unit = {
    if (!retryBuffer.isEmpty) {
      retryStore()
    }

    repository.store(batch)
  }

  protected def retryStore() = {
    try {
      for (batch ← retryBuffer) { repository.store(batch) }
      log.info("Stored [%s] retry batches" format retryBuffer.size)
      retryBuffer = Vector[Batch]()
    } catch {
      case e: Exception ⇒
        log.error(e, "Retry failed. %s" format e.getMessage)
    }
  }

  protected def addToRetryBuffer(batch: Batch) = {
    if (retryBuffer.size < retryBufferSize) {
      retryBuffer = retryBuffer :+ batch
    }
  }

}
