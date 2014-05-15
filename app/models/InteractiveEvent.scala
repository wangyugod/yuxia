package models

import scala.slick.driver.MySQLDriver.simple._
import java.sql.Timestamp

/**
 * Created by thinkpad-pc on 14-5-2.
 */
case class InteractiveEvent(id: String, title: String, name: String, description: String, profileId: String, state: Int, supportedQty: Int, createdTime: Timestamp, lastModifiedTime: Timestamp) {

}

case class InteractiveEventReply(id: String, eventId: String, message: String, replier: String, createdTime: Timestamp)

class InteractiveEventRepo(tag: Tag) extends Table[InteractiveEvent](tag, "interactive_event") {
  def id = column[String]("id", O.PrimaryKey)

  def title = column[String]("title")

  def name = column[String]("name")

  def description = column[String]("description")

  def profileId = column[String]("user_id")

  def state = column[Int]("state")

  def supportedQty = column[Int]("supported_qty")

  def createdTime = column[Timestamp]("created_time")

  def lastModifiedTime = column[Timestamp]("last_modified_time")

  override def * = (id, title, name, description, profileId, state, supportedQty, createdTime, lastModifiedTime) <>(InteractiveEvent.tupled, InteractiveEvent.unapply)
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
  val WAITING_FOR_APPROVE = 0
  val APPROVED = 1
  val PROCESSING = 2
  val DONE = 3
  val REMOVED = 4
}

object InteractiveEvent extends ((String, String, String, String, String, Int, Int, Timestamp, Timestamp) => InteractiveEvent) {
  val ieRepo = TableQuery[InteractiveEventRepo]

  def createInteractiveEvent(ie: InteractiveEvent)(implicit session: Session) = {
    ieRepo.insert(ie)
  }

  def all()(implicit session: Session) = {
    ieRepo.list()
  }
}




