package controllers

import play.api.mvc._
import models._
import play.api.i18n.Messages
import views._

object Application extends Controller with Users {


  def index = Action {
    implicit request => {
      val users = Profile.findAllUsers()
      Ok(html.index(Messages("site.name"), users))
    }
  }

  def pageNotFound = Action{
    implicit request =>{
      BadRequest(html.pageNotFound())
    }
  }

}