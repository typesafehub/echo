/**
 *  Copyright (C) 2011-2013 Typesafe <http://typesafe.com/>
 */
package com.typesafe.atmos.subscribe

import com.typesafe.atmos.uuid.UUID

case class Notification(timestamp: Long, uuid: UUID)

case class Notifications(events: Seq[Notification])
