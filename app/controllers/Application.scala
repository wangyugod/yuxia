package controllers

import play.api._
import play.api.mvc._
import models._
import play.api.i18n.Messages

object Application extends Controller with Users {


  def index = Action {
    implicit request =>{
      val users = Profile.findAllUsers()
      Ok(views.html.index(Messages("site.name"), users))
    }

  }

}