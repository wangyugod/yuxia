package controllers

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import validation.Constraints
import views.html

import models._
import util._
import play.api.Logger
import play.api.i18n.Messages

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
  val MERCHANT_DESC = "merch_desc"

  implicit def merchant(implicit session: Session): Option[Merchant] = {
    session.get(MERCHANT_LOGIN) match {
      case Some(login) => Some(Merchant(session.get(MERCHANT_ID).get, session.get(MERCHANT_LOGIN).get, "", session.get(MERCHANT_NAME).get, session.get(MERCHANT_DESC)))
      case None => None
    }
  }
}

object Merchandise extends Controller with Merchants {

  private val log = Logger(this.getClass)


  val loginForm: Form[(String, String)] = Form(
    tuple(
      "login" -> nonEmptyText.verifying(email.constraints.head),
      "password" -> nonEmptyText
    )
  )

  val merchantForm: Form[Merchant] = Form(
    mapping(
      "id" -> optional(text),
      "login" -> email,
      "description" -> optional(text),
      "password" -> tuple(
        "main" -> text.verifying(Messages("error.password.minlength"), {
          _.length >= 6}),
        "confirm" -> text
      ).verifying(
        // Add an additional constraint: both passwords must match
        Messages("error.password.notmatch"), passwords => passwords._1 == passwords._2
      ),
      "name" -> nonEmptyText
    )
      ((id, login, description, passwords, name) => Merchant(id.getOrElse(IdGenerator.generateProfileId), login, passwords._1, name, description))
      ((merchant: Merchant) => {
        Some(Some(merchant.id), merchant.login, merchant.description, (merchant.password, ""), merchant.name)
      }) verifying(Messages("error.login.alreadyexist"), profile => Merchant.findByLogin(profile.login).isEmpty
      )
  )

  def create = Action {
    implicit request => {
      merchantForm.bindFromRequest().fold(
        formWithErrors => {
          if (log.isDebugEnabled)
            log.debug("error form is \n" + formWithErrors)
          BadRequest(html.merchandise.merchreg(formWithErrors))
        },
        merchant => {
          Merchant.create(merchant)
          merchantForm.fill(merchant)
          Redirect(routes.Products.list()).withSession(MERCHANT_LOGIN -> merchant.login, MERCHANT_ID -> merchant.id, MERCHANT_NAME -> merchant.name, MERCHANT_DESC -> merchant.description.getOrElse(""))
        }
      )
    }
  }

  def authenticate = Action {
    implicit request => {
      loginForm.bindFromRequest().fold(
        formWithErrors => {
          BadRequest(html.merchandise.merchlogin(formWithErrors))
        },
        merchant => {
          val loginMerchant = Merchant.authenticateUser(merchant._1, merchant._2)
          if (loginMerchant.isDefined) {
            Redirect(routes.Products.list()).withSession(MERCHANT_LOGIN -> merchant._1, MERCHANT_ID -> loginMerchant.get.id, MERCHANT_NAME -> loginMerchant.get.name, MERCHANT_DESC -> loginMerchant.get.description.getOrElse(""))
          } else {
            BadRequest(html.merchandise.merchlogin(loginForm.withError("error", "invalid login or password")))
          }
        }
      )
    }
  }


  def login = Action {
    implicit request => {
      Ok(html.merchandise.merchlogin(loginForm))
    }
  }

  def signup = Action {
    implicit request => {
      Ok(html.merchandise.merchreg(merchantForm))
    }
  }

  def logout = Action {
    implicit request => {
      Redirect(routes.Merchandise.login()).withNewSession
    }
  }


  /*def productInfo = Action {
    implicit request =>
      Ok(html.merchandise.product())
  }*/


}
