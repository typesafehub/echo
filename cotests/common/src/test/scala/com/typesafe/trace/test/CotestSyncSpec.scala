/**
 *  Copyright (C) 2009-2012 Typesafe Inc. <http://www.typesafe.com>
 */

package com.typesafe.trace.test

import java.io._
import org.scalatest.BeforeAndAfterAll
import org.scalatest.WordSpec

object CotestSyncSpec {
  import FileBasedBarrier.{ DefaultTimeout, DefaultSleep }

  val CotestDir = new File(".cotest")
  val BarrierDir = new File(CotestDir, "barrier")
  val PropertiesDir = new File(CotestDir, "properties")

  val StartBarrier = "cotest-start"
  val EndBarrier = "cotest-end"

  def start(timeFactor: Int, className: String, nodeName: String, count: Int) = barrier(timeFactor, StartBarrier, count, className, nodeName)

  def end(timeFactor: Int, className: String, nodeName: String, count: Int) = barrier(timeFactor, EndBarrier, count, className, nodeName)

  def barrier(timeFactor: Int, name: String, count: Int, className: String, nodeName: String, timeout: Long = DefaultTimeout, sleep: Long = DefaultSleep, onSpin: ⇒ Unit = ()) = {
    new FileBasedBarrier(timeFactor, name, count, className, nodeName, BarrierDir, timeout, sleep, onSpin).await()
  }

  def writeProperties(file: File, map: Map[String, Any]): Unit = {
    file.getParentFile.mkdirs()
    writeToFile(file) { writer ⇒
      for ((key, value) ← map) {
        writer.write(key + "=" + value.toString)
        writer.newLine
      }
    }
  }

  def readProperties(file: File, clear: Boolean = true): Map[String, String] = {
    var map = Map.empty[String, String]
    if (file.exists) {
      readFromFile(file) { reader ⇒
        var line = reader.readLine
        while (line ne null) {
          val kv = line split '='
          if (kv.length == 2) map += kv(0) -> kv(1)
          line = reader.readLine
        }
      }
      if (clear) file.delete()
    }
    map
  }

  def getProperty[T](properties: Map[String, String], name: String)(convert: String ⇒ T): T = {
    val value = try { properties get name map convert } catch {
      case e: Exception ⇒ throw new Exception("Couldn't convert simulation property: " + name, e)
    }
    value getOrElse { throw new Exception("Missing simulation property: " + name) }
  }

  def writeToFile(file: File)(writeTo: BufferedWriter ⇒ Unit) = {
    val writer = new BufferedWriter(new FileWriter(file))
    try writeTo(writer) finally writer.close()
  }

  def readFromFile[T](file: File)(readFrom: BufferedReader ⇒ T): T = {
    val reader = new BufferedReader(new FileReader(file))
    try readFrom(reader) finally reader.close()
  }
}

trait CotestSyncSpec extends WordSpec with BeforeAndAfterAll {
  import FileBasedBarrier.{ DefaultTimeout, DefaultSleep }

  def nodes: Int
  def nodeName: String

  def timeFactor: Int

  override def beforeAll(): Unit = {
    super.beforeAll()
    onStart()
    CotestSyncSpec.start(timeFactor, getClass.getName, nodeName, nodes)
  }

  def onStart(): Unit = {}

  override def afterAll(): Unit = {
    super.afterAll()
    CotestSyncSpec.end(timeFactor, getClass.getName, nodeName, nodes)
    onEnd()
  }

  def onEnd(): Unit = {}

  def barrier(name: String, timeout: Long = DefaultTimeout, sleep: Long = DefaultSleep, onSpin: ⇒ Unit = ()) = {
    CotestSyncSpec.barrier(timeFactor, name, nodes, getClass.getName, nodeName, timeout, sleep, onSpin)
  }

  def writeProperties(name: String, map: Map[String, Any]): Unit = {
    val file = new File(new File(CotestSyncSpec.PropertiesDir, getClass.getName), name)
    CotestSyncSpec.writeProperties(file, map)
  }

  def readProperties(name: String, delete: Boolean = true): Map[String, String] = {
    val file = new File(new File(CotestSyncSpec.PropertiesDir, getClass.getName), name)
    CotestSyncSpec.readProperties(file, delete)
  }

  def getProperty[T](properties: Map[String, String], name: String)(convert: String ⇒ T): T = {
    CotestSyncSpec.getProperty(properties, name)(convert)
  }
}
