/**
 *  Copyright (C) 2011-2013 Typesafe <http://typesafe.com/>
 */
package com.typesafe.trace.subscribe

import com.typesafe.trace.uuid.UUID

case class Notification(timestamp: Long, uuid: UUID)

case class Notifications(events: Seq[Notification])
