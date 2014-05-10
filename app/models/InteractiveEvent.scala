package models

import scala.slick.driver.MySQLDriver.simple._
import java.sql.Timestamp

/**
 * Created by thinkpad-pc on 14-5-2.
 */
case class InteractiveEvent(id: String, title: String, description: String, profileId: String, state: Int, supportedQty: Int, createdTime: Timestamp, lastModifiedTime: Timestamp) {

}

case class InteractiveEventReply(id: String, eventId: String, message: String, replier: String, createdTime: Timestamp)

class InteractiveEventRepo(tag: Tag) extends Table[InteractiveEvent](tag, "interactive_event") {
  def id = column[String]("id", O.PrimaryKey)

  def title = column[String]("title")

  def description = column[String]("description")

  def profileId = column[String]("user_id")

  def state = column[Int]("state")

  def supportedQty = column[Int]("supported_qty")

  def createdTime = column[Timestamp]("created_time")

  def lastModifiedTime = column[Timestamp]("last_modified_time")

  override def * = (id, title, description, profileId, state, supportedQty, createdTime, lastModifiedTime) <>(InteractiveEvent.tupled, InteractiveEvent.unapply)
}

class InteractiveEventReplyRepo(tag: Tag) extends Table[InteractiveEventReply](tag, "interactive_event_rep") {
  def id = column[String]("id", O.PrimaryKey)

  def eventId = column[String]("event_id")

  def message = column[String]("message")

  def replier = column[String]("replier")

  def createdTime = column[Timestamp]("created_time")

  def * = (id, eventId, message, replier, createdTime) <>(InteractiveEventReply.tupled, InteractiveEventReply.unapply)
}


object InteractiveEventState {
  val RAISED_UP = 0
  val PROCESSING = 1
  val DONE = 2
  val REMOVED = 3
}




