package controllers

import play.api.mvc._
import views.html
import play.api.data.Form
import models._
import play.api.data.Forms._
import helper.{AppHelper, IdGenerator}
import play.api.data.validation.Constraints

/**
 * Created with IntelliJ IDEA.
 * User: Administrator
 * Date: 13-6-28
 * Time: 上午8:17
 * To change this template use File | Settings | File Templates.
 */

trait Merchants {
  val MERCHANT_LOGIN = "merch_login"
  val MERCHANT_NAME = "merch_name"
  val MERCHANT_NUMBER = "merch_num"
  val MERCHANT_ID = "merch_id"

  implicit def merchant(implicit session: Session): Option[Merchant] = {
    println("default user method")
    session.get(MERCHANT_LOGIN) match {
      case Some(login) => Some(Merchant(session.get(MERCHANT_ID).get, session.get(MERCHANT_LOGIN).get, "", session.get(MERCHANT_NUMBER).get, session.get(MERCHANT_NAME).get))
      case None => None
    }
  }
}

object Merchandise extends Controller with Merchants {


  def loginForm(request: Request[_]): Form[(String, String)] = Form(
    tuple(
      "login" -> nonEmptyText.verifying(email.constraints.head),
      "password" -> text
    ) verifying("Invalid login or password", result => result match {
      case (login, password) => {
        val loginMerchant = Merchant.authenticateUser(login, password)

        //        request.session
        if (loginMerchant.isDefined) {
          request.session +(MERCHANT_ID, loginMerchant.get.id) +(MERCHANT_NAME, loginMerchant.get.name) +(MERCHANT_NUMBER, loginMerchant.get.merchantNum) +(MERCHANT_LOGIN, loginMerchant.get.login)
          true
        } else {
          false
        }
      }
    })
  )

  val merchantForm: Form[Merchant] = Form(
    mapping(
      "id" -> optional(text),
      "login" -> email,
      "password" -> tuple(
        "main" -> text(minLength = 6),
        "confirm" -> text
      ).verifying(
        // Add an additional constraint: both passwords must match
        "Passwords don't match", passwords => passwords._1 == passwords._2
      ),
      "name" -> nonEmptyText,
      "merchantNum" -> optional(text)
    )
      ((id, login, passwords, name, merchantNum) => Merchant(id.getOrElse(IdGenerator.generateProfileId), login, passwords._1, name, merchantNum.getOrElse("")))
      ((merchant: Merchant) => {
        Some(Some(merchant.id), merchant.login, (merchant.password, ""), merchant.name, Some(merchant.merchantNum))
      }) //verifying("User with the same loign already exists", profile => ProfileService.findUserByLogin(profile.login).isEmpty)
  )

  def authenticate = Action {
    implicit request => {
      val form = loginForm(request)
      form.bindFromRequest().fold(
        formWithErrors => {
          BadRequest(html.login(formWithErrors))
        },
        merchant => {
          Redirect(routes.Application.index()).withSession(MERCHANT_LOGIN -> merchant._1)
        }
      )
    }
  }


  def login = Action {
    implicit request =>{
      val form = loginForm(request)
      Ok(html.merchandise.merchlogin(form))
    }
  }


  /*def productInfo = Action {
    implicit request =>
      Ok(html.merchandise.product())
  }*/


}
