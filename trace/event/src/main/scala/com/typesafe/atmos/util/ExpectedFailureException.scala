/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */
package com.typesafe.atmos.util

class ExpectedFailureException(msg: String) extends RuntimeException("Expected error: " + msg)
