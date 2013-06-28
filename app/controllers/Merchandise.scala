package controllers

import play.api.mvc._
import views.html
import play.api.data.Form
import models._
import play.api.data.Forms._
import helper.{AppHelper, IdGenerator}

/**
 * Created with IntelliJ IDEA.
 * User: Administrator
 * Date: 13-6-28
 * Time: 上午8:17
 * To change this template use File | Settings | File Templates.
 */

trait Merchants {
  val LOGIN_KEY = "merchant_login"


  implicit def user(implicit session: Session): Option[Merchant] = {
    println("default user method")
    session.get(LOGIN_KEY) match {
      case Some(login) => Merchant()
      case None => None
    }
  }
}

object Merchandise extends Controller with Merchants{



  def loginForm(request:Request[_]) : Form[(String, String)] = Form(
    tuple(
      "login" -> nonEmptyText.verifying(email.constraints.head),
      "password" -> text
    ) verifying("Invalid login or password", result => result match {
      case (login, password) => {
        val loginMerchant = Merchant.authenticateUser(login, password)
//        request.session
        loginMerchant.isDefined
      }
    })
  )

  def authenticate = Action {
    implicit request =>
      loginForm.bindFromRequest.fold(
        formWithErrors => {
          BadRequest(html.login(formWithErrors))
        },
        merchant => {
          Redirect(routes.Application.index()).withSession(LOGIN_KEY -> merchant._1)
        }
      )
  }



  def productInfo = Action {
    implicit request =>
      Ok(html.merchandise.product())
  }



}
