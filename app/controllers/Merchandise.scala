package controllers

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import validation.Constraints
import views.html

import models._
import helper._

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
    println("current session is " + session)
    session.get(MERCHANT_LOGIN) match {
      case Some(login) => Some(Merchant(session.get(MERCHANT_ID).get, session.get(MERCHANT_LOGIN).get, "", session.get(MERCHANT_NUMBER).get, session.get(MERCHANT_NAME).get))
      case None => None
    }
  }
}

object Merchandise extends Controller with Merchants {


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
      }) verifying("User with the same loign already exists", profile => Merchant.findByLogin(profile.login).isEmpty)
  )

  def create = Action {
    implicit request => {
      merchantForm.bindFromRequest().fold(
        formWithErrors => {
          BadRequest(html.merchandise.merchreg(formWithErrors))
        },
        merchant => {
          Merchant.create(merchant)
          merchantForm.fill(merchant)
          Redirect(routes.Merchandise.signup())
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
          if(loginMerchant.isDefined){
            Redirect(routes.Application.index()).withSession(MERCHANT_LOGIN -> merchant._1, MERCHANT_ID -> loginMerchant.get.id, MERCHANT_NAME -> loginMerchant.get.name, MERCHANT_NUMBER ->  loginMerchant.get.merchantNum)
          } else {
            BadRequest(html.merchandise.merchlogin(loginForm))
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


  /*def productInfo = Action {
    implicit request =>
      Ok(html.merchandise.product())
  }*/


}
