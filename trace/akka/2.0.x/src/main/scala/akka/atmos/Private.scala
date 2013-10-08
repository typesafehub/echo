/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */
package akka.atmos

import akka.actor.ActorSystem

/*
 * For accessing akka package private code.
 */
object Private {
  def findClassLoader() = ActorSystem.findClassLoader()
}
