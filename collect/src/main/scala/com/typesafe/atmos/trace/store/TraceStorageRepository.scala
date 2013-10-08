/**
 *  Copyright (C) 2011-2013 Typesafe <http://typesafe.com/>
 */
package com.typesafe.atmos.trace
package store

trait TraceStorageRepository {
  def store(batch: Batch): Unit
}
