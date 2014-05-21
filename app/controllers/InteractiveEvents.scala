package controllers

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import models.{InteractiveEventState, LocalIdGenerator, InteractiveEvent}
import java.sql.Timestamp
import views.html
import util.DBHelper

/**
 * Created by thinkpad-pc on 14-5-13.
 */
object InteractiveEvents extends Controller with Users with Secured {

  val interactiveEventForm = Form(
    mapping(
      "id" -> optional(text),
      "userId" -> nonEmptyText,
      "title" -> nonEmptyText,
      "name" -> nonEmptyText,
      "description" -> text
    )((id, userId, title, name, description) => InteractiveEvent(id.getOrElse(LocalIdGenerator.generateInteractiveEventId()), title, name, description, userId, InteractiveEventState.WAITING_FOR_APPROVE, 1, new Timestamp(new java.util.Date().getTime), new Timestamp(new java.util.Date().getTime)))
      ((ie: InteractiveEvent) => Some((Some(ie.id), ie.profileId, ie.title, ie.name, ie.description)))
  )

  def wantToEat() = Action {
    implicit request =>
      val eventList = DBHelper.database.withSession {
        implicit session =>
          InteractiveEvent.all()
      }
      Ok(html.browse.wanttoeat(eventList))
  }


  def createInteractiveEvent = isAuthenticated {
    implicit request =>
      interactiveEventForm.bindFromRequest().fold(
        formWithErrors => {
          Redirect(routes.InteractiveEvents.wantToEat()).flashing("ieError" -> "ie.error")
        },
        interactiveEvent => DBHelper.database.withTransaction {
          implicit session =>
            InteractiveEvent.createInteractiveEvent(interactiveEvent)
            Redirect(routes.InteractiveEvents.wantToEat())
        }
      )
  }

  def supportEvent(id: String) = Action {
    implicit request =>
      DBHelper.database.withTransaction {
        implicit session =>
          InteractiveEvent.supportEvent(id)
      }
      Ok
  }


}
