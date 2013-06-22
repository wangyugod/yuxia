package controllers

import play.api._
import play.api.mvc._
import models._

object Application extends Controller with Users {


  def index = Action {
    implicit request =>
      Ok(views.html.index("中文测试"))
  }

}