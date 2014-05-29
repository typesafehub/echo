/**
 *  Copyright (C) 2011-2013 Typesafe <http://typesafe.com/>
 */
package com.typesafe.trace.receive

import com.google.protobuf.CodedInputStream
import com.typesafe.trace.{ InternalSystem, Batch, TraceEventHandler, TraceProtocol, TraceReceiver }
import com.typesafe.trace.util.ProtobufConverter
import java.net.{ ServerSocket, Socket }
import java.util.concurrent.{ Executors, TimeUnit }

class ProtobufTraceReceiver(val eventHandler: TraceEventHandler, port: Int, maxConnections: Int) extends TraceReceiver {

  private[this] val server = Executors.newSingleThreadExecutor()
  private[this] val listener = new ServerSocket(port)
  private[this] val connections = Executors.newFixedThreadPool(maxConnections)
  private[this] var sockets = Set.empty[Socket]

  server.execute(new Runnable {
    def run = {
      try {
        while (true) {
          val socket = listener.accept()
          addConnection(socket)
        }
      } catch {
        case e: Exception ⇒ // assume shutting down
          closeAllConnections()
      }
    }
  })

  private def addConnection(socket: Socket): Unit = {
    val added = synchronized {
      val addable = sockets.size < maxConnections
      if (addable) sockets += socket
      addable
    }
    if (added) {
      connections.execute(new Runnable {
        def run = receiveOn(socket)
      })
    } else {
      socket.close()
      InternalSystem.enabled { system ⇒
        system.log.warning(s"Couldn't accept new trace receiver connection - already at max connections: $maxConnections")
      }
    }
  }

  private def closeConnection(socket: Socket): Unit = {
    synchronized { sockets -= socket }
    socket.close()
  }

  private def closeAllConnections(): Unit = synchronized {
    sockets foreach { _.close() }
    sockets = Set.empty[Socket]
  }

  private def receiveOn(socket: Socket): Unit = {
    val input = CodedInputStream.newInstance(socket.getInputStream)
    try {
      while (true) {
        val size = input.readRawVarint32
        val oldLimit = input.pushLimit(size)
        val message = TraceProtocol.ProtoBatch.parseFrom(input)
        val batch = ProtobufConverter.fromProto(message)
        eventHandler.publish(batch)
        input.popLimit(oldLimit)
        input.resetSizeCounter()
      }
    } catch {
      case e: Exception ⇒ // assume disconnected or shutting down
        closeConnection(socket)
    }
  }

  def shutdown(): Unit = {
    listener.close()
    server.shutdown()
    server.awaitTermination(1, TimeUnit.SECONDS)
    connections.shutdown()
    connections.awaitTermination(1, TimeUnit.SECONDS)
    eventHandler.shutdown()
  }
}
