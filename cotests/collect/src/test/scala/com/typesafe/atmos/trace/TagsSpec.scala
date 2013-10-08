/**
 *  Copyright (C) 2011-2013 Typesafe, Inc <http://typesafe.com>
 */

package com.typesafe.atmos.trace

import com.typesafe.atmos.test.AtmosCollectSpec

class Akka20TagsSpec extends TagsSpec {
  val eventCount = 81
}

class Akka21TagsSpec extends TagsSpec {
  val eventCount = 47
}

class Akka22Scala210TagsSpec extends Akka22TagsSpec
class Akka22Scala211TagsSpec extends Akka22TagsSpec

abstract class Akka22TagsSpec extends TagsSpec {
  val eventCount = 47
}

abstract class TagsSpec extends AtmosCollectSpec {
  def eventCount: Int

  "Tags" must {

    "be attached to actor info" in {
      eventCheck(expected = eventCount) {
        for (created ← annotationsOf[ActorCreated]) {
          if (created.info.path contains "tags1") {
            created.info.tags must be(Set("all", "default", "tags", "1"))
          } else if (created.info.path contains "tags2") {
            created.info.tags must be(Set("all", "default", "tags", "2"))
          }
        }
      }
    }

  }
}
