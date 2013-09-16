package controllers

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import validation.Constraints
import views.html

import models._
import util._
import play.api.i18n.Messages
import play.api.Logger

/**
 * Created with IntelliJ IDEA.
 * User: simonwang
 * Date: 6/13/13
 * Time: 11:24 PM
 * To change this template use File | Settings | File Templates.
 */

trait Users {
  val LOGIN_KEY = "user_login"
  val USER_NAME = "user_name"
  val USER_ID = "user_id"

  implicit def toUser(implicit session: Session): Option[Profile] = {
    session.get(LOGIN_KEY) match {
      case Some(login) => Some(Profile(session.get(USER_ID).get, session.get(LOGIN_KEY).get, "", session.get(USER_NAME).get, None, None))
      case None => None
    }
  }
}

object ProfileController extends Controller with Users with Secured {
  private val log = Logger(this.getClass)


  val profileForm: Form[Profile] = Form(
    mapping(
      "id" -> optional(text),
      "login" -> email,
      "password" -> tuple(
        "main" -> text.verifying(Messages("error.password.minlength"), {
          _.length >= 6
        }),
        "confirm" -> text
      ).verifying(
        // Add an additional constraint: both passwords must match
        Messages("error.password.notmatch"), passwords => passwords._1 == passwords._2
      ),
      "name" -> nonEmptyText
    )
      ((id, login, passwords, name) => Profile(id.getOrElse(IdGenerator.generateProfileId), login, passwords._1, name, None, None))
      ((profile: Profile) => {
        Some((Some(profile.id), profile.login, (profile.password, ""), profile.name))
      }) verifying(Messages("error.login.alreadyexist"), profile => Profile.findUserByLogin(profile.login).isEmpty)
  )

  val profileUpdateForm: Form[Profile] = Form(
    mapping(
      "id" -> optional(text),
      "login" -> email,
      "name" -> nonEmptyText,
      "gender" -> optional(text),
      "birthday" -> optional(text.verifying(Constraints.pattern( """(19|20)\d\d[-](0[1-9]|1[012])[-](0[1-9]|[12][0-9]|3[01])""".r, "Date Constraint", Messages("error.myacct.birthday"))))
    )
      ((id, login, name, gender, birthday) => Profile(id.getOrElse(IdGenerator.generateProfileId), login, "", name, gender, AppHelper.convertDateFromText(birthday)))
      ((profile: Profile) => {
        Some((Some(profile.id), profile.login, profile.name, profile.gender, AppHelper.convertDateToText(profile.birthDay)))
      }) //verifying("User with the same loign already exists", profile => ProfileService.findUserByLogin(profile.login).isEmpty)
  )

  val loginForm: Form[(String, String)] = Form(
    tuple(
      "login" -> email,
      "password" -> nonEmptyText
    )
  )

  val addressForm: Form[Address] = Form(
    mapping(
      "id" -> optional(text),
      "province" -> nonEmptyText,
      "city" -> nonEmptyText,
      "district" -> nonEmptyText,
      "contactPhone" -> nonEmptyText,
      "addressLine" -> nonEmptyText,
      "contactPerson" -> nonEmptyText,
      "areaId" -> optional(text)
    )((id, province, city, district, contactPhone, addressLine, contactPerson, areaId) => Address(id.getOrElse(IdGenerator.generateAddressId()), province, city, district, contactPhone, addressLine, contactPerson, areaId))
      ((addr: Address) => Some(Some(addr.id), addr.province, addr.city, addr.district, addr.contactPhone, addr.addressLine, addr.contactPerson, addr.areaId))
  )

  def login = Action {
    implicit request =>
      Ok(html.login(loginForm)).withCookies(Cookie("DG", scala.util.Random.nextString(2)))
  }

  def authenticate = Action {
    implicit request =>
      loginForm.bindFromRequest.fold(
        formWithErrors => {
          BadRequest(html.login(formWithErrors))
        },
        user => {
          val foundUser = Profile.authenticateUser(user._1, user._2)
          if (foundUser.isDefined) {
            Redirect(routes.Application.index()).withSession(loginKey -> foundUser.get.login, USER_NAME -> foundUser.get.name, USER_ID -> foundUser.get.id)
          } else {
            BadRequest(html.login(loginForm.withError("login.failed", Messages("login.failed.msg"))))
          }

        }
      )
  }

  def signup = Action {
    implicit request =>
      Ok(html.signup(profileForm))
  }

  def create = Action {
    implicit request => profileForm.bindFromRequest.fold(
      formWithErrors => {
        if (log.isDebugEnabled)
          log.debug("Error form is " + formWithErrors)
        BadRequest(html.signup(formWithErrors))
      },
      profile => {
        Profile.createUser(profile)
        Redirect(routes.Application.index()).withSession(loginKey -> profile.login, USER_NAME -> profile.name, USER_ID -> profile.id)
      }
    )
  }

  def update = Action {
    implicit request => profileUpdateForm.bindFromRequest.fold(
      formWithErrors => {
        if(log.isDebugEnabled)
          log.debug("error form is \n" + formWithErrors.errors)
        BadRequest(html.myaccount.baseinfo(formWithErrors))
      },
      profile => {
        val oldProfile = Profile.findUserByLogin(profile.login).get
        val newProfile: Profile = Profile(profile.id, profile.login, oldProfile.password, profile.name, profile.gender, profile.birthDay)
        Profile.updateUser(newProfile)
        Redirect(routes.ProfileController.myAccount())
      }
    )
  }

  def myAccount = isAuthenticated(
    implicit request =>{
      val profile = Profile.findUserByLogin(request.session.get(loginKey).get)
      Ok(html.myaccount.baseinfo(profileUpdateForm.fill(profile.get)))
    }
  )

  def signout = Action {
    implicit request =>
      Redirect(routes.ProfileController.login()).withNewSession
  }

  def addressList = isAuthenticated {
    implicit request =>{
      val userId = request.session.get(USER_ID).get
      val addresses = Address.userAddresses(userId)
      if(log.isDebugEnabled)
        log.debug("address list: " + addresses)
      Ok(html.myaccount.addressmgt(addressForm, addresses))
    }
  }

  def updateAddress = isAuthenticated{
    implicit request => {
      addressForm.bindFromRequest.fold(
        formWithErrors => {
          val userId = request.session.get(USER_ID).get
          val addresses = Address.userAddresses(userId)
          BadRequest(html.myaccount.addressmgt(formWithErrors, addresses))
        },
        address => {
          val userId = request.session.get(USER_ID).get
          Address.saveOrUpdate(userId, address)
          Redirect(routes.ProfileController.addressList())
        }
      )
    }
  }


}
