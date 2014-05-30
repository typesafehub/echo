/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */

package com.typesafe.trace

import com.typesafe.trace.test.EchoCollectSpec

class Akka20TagsSpec extends TagsSpec {
  val eventCount = 81
}

class Akka21TagsSpec extends TagsSpec {
  val eventCount = 47
}

class Akka22TagsSpec extends TagsSpec {
  val eventCount = 47
}

class Akka23Scala210TagsSpec extends Akka23TagsSpec
class Akka23Scala211TagsSpec extends Akka23TagsSpec

abstract class Akka23TagsSpec extends TagsSpec {
  val eventCount = 47
}

abstract class TagsSpec extends EchoCollectSpec {
  def eventCount: Int

  "Tags" must {

    "be attached to actor info" in {
      eventCheck(expected = eventCount) {
        for (created ← annotationsOf[ActorCreated]) {
          if (created.info.path contains "tags1") {
            created.info.tags should be(Set("all", "default", "tags", "1"))
          } else if (created.info.path contains "tags2") {
            created.info.tags should be(Set("all", "default", "tags", "2"))
          }
        }
      }
    }

  }
}
