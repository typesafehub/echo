/**
 *  Copyright (C) 2009-2012 Typesafe Inc. <http://www.typesafe.com>
 */

package com.typesafe.trace.test

import java.io.File
import System.{ currentTimeMillis ⇒ now }

class BarrierTimeoutException(message: String) extends RuntimeException(message)

object FileBasedBarrier {
  val HomeDir = new File(".barrier")
  val DefaultTimeout = 60000 // 60 seconds
  val DefaultSleep = 100 // 100 milliseconds
}

import FileBasedBarrier._

class FileBasedBarrier(
  timeFactor: Int,
  name: String,
  count: Int,
  group: String,
  node: String,
  home: File = FileBasedBarrier.HomeDir,
  timeout: Long = FileBasedBarrier.DefaultTimeout,
  sleep: Long = FileBasedBarrier.DefaultSleep,
  onEntrySpin: ⇒ Unit = ()) {

  val barrierDir = {
    val dir = new File(new File(home, group), name)
    dir.mkdirs()
    dir
  }

  val nodeFile = new File(barrierDir, node)

  val readyFile = new File(barrierDir, "ready")

  def await() = { enter(); leave() }

  def apply(body: ⇒ Unit) {
    enter()
    body
    leave()
  }

  def enter() = {
    createNode()
    if (nodesPresent >= count) createReady()
    def readyTest = { onEntrySpin; readyFile.exists }
    val ready = waitFor(readyTest, timeout * timeFactor, sleep)
    if (!ready) expire("entry")
  }

  def leave() = {
    removeNode()
    val empty = waitFor(nodesPresent <= 1, timeout * timeFactor, sleep)
    removeReady()
    if (!empty) expire("exit")
  }

  def nodesPresent = barrierDir.list.size

  def createNode() = nodeFile.createNewFile()

  def removeNode() = nodeFile.delete()

  def createReady() = readyFile.createNewFile()

  def removeReady() = readyFile.delete()

  def waitFor(test: ⇒ Boolean, timeout: Long, sleep: Long): Boolean = {
    val start = now
    val limit = start + (timeout * timeFactor)
    var passed = test
    var expired = false
    while (!passed && !expired) {
      if (now > limit) expired = true
      else {
        Thread.sleep(sleep)
        passed = test
      }
    }
    passed
  }

  def expire(barrier: String) = {
    throw new BarrierTimeoutException("Timeout [%s] waiting for %s barrier [%s]" format (timeout * timeFactor, barrier, name))
  }
}
