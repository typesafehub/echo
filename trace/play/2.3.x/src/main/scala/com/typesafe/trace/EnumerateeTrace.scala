/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */

package com.typesafe.trace

import com.typesafe.trace.util.Uuid
import play.api.libs.iteratee.{ Iteratee, Step, Input }
import scala.concurrent.Future
import scala.util.{ Try, Success, Failure }

trait EnumerateeTagData {
  def chunking: Boolean
}

trait EnumerateeTag {
  def tag: EnumerateeTagData
}

private[trace] object EnumerateeTrace {
  val Chunking = new EnumerateeTagData {
    val chunking: Boolean = true
  }
}
