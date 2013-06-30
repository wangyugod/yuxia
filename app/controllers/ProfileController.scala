package controllers

import play.api.mvc.Controller
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import validation.Constraints
import views.html

import models._
import helper._
import play.api.i18n.Messages

/**
 * Created with IntelliJ IDEA.
 * User: simonwang
 * Date: 6/13/13
 * Time: 11:24 PM
 * To change this template use File | Settings | File Templates.
 */

trait Users {
  val LOGIN_KEY = "login"

  implicit def user(implicit session: Session): Option[Profile] = {
    println("default user method")
    session.get(LOGIN_KEY) match {
      case Some(login) => Profile.findUserByLogin(login)
      case None => None
    }
  }
}

object ProfileController extends Controller with Users {


  val profileForm: Form[Profile] = Form(
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
      "gender" -> optional(text),
      "birthday" -> optional(text.verifying(Constraints.pattern( """(19|20)\d\d[-](0[1-9]|1[012])[-](0[1-9]|[12][0-9]|3[01])""".r, "Date Constraint", "Your input should be in format YYYY-MM-DD")))
    )
      ((id, login, passwords, name, gender, birthday) => Profile(id.getOrElse(IdGenerator.generateProfileId), login, passwords._1, name, gender, AppHelper.convertBirthdayFromText(birthday)))
      ((profile: Profile) => {
        Some((Some(profile.id), profile.login, (profile.password, ""), profile.name, profile.gender, AppHelper.convertBirthdayToText(profile.birthDay)))
      }) verifying("User with the same loign already exists", profile => Profile.findUserByLogin(profile.login).isEmpty)
  )

  val profileUpdateForm: Form[Profile] = Form(
    mapping(
      "id" -> optional(text),
      "login" -> email,
      "name" -> nonEmptyText,
      "gender" -> optional(text),
      "birthday" -> optional(text.verifying(Constraints.pattern( """(19|20)\d\d[-](0[1-9]|1[012])[-](0[1-9]|[12][0-9]|3[01])""".r, "Date Constraint", "Your input should be in format YYYY-MM-DD")))
    )
      ((id, login, name, gender, birthday) => Profile(id.getOrElse(IdGenerator.generateProfileId), login, "", name, gender, AppHelper.convertBirthdayFromText(birthday)))
      ((profile: Profile) => {
        Some((Some(profile.id), profile.login, profile.name, profile.gender, AppHelper.convertBirthdayToText(profile.birthDay)))
      }) //verifying("User with the same loign already exists", profile => ProfileService.findUserByLogin(profile.login).isEmpty)
  )

  val loginForm: Form[(String, String)] = Form(
    tuple(
      "login" -> nonEmptyText.verifying(email.constraints.head),
      "password" -> text
    ) verifying(Messages("login.failed.msg"), result => result match {
      case (login, password) => Profile.authenticateUser(login, password).isDefined
    })
  )

  def login = Action {
    implicit request =>
      Ok(html.login(loginForm))
  }

  def authenticate = Action {
    implicit request =>
      loginForm.bindFromRequest.fold(
        formWithErrors => {
          BadRequest(html.login(formWithErrors))
        },
        user => Redirect(routes.Application.index()).withSession(LOGIN_KEY -> user._1)
      )
  }

  def signup = Action {
    implicit request =>
      Ok(html.signup(profileForm))
  }

  def create = Action {
    implicit request => profileForm.bindFromRequest.fold(
      formWithErrors => {
        BadRequest(html.signup(formWithErrors))
      },
      profile => {
        Profile.createUser(profile)
        Redirect(routes.Application.index()).withSession(LOGIN_KEY -> profile.login)
      }
    )
  }

  def update = Action {
    implicit request => profileUpdateForm.bindFromRequest.fold(
      formWithErrors => {
        BadRequest(html.basicInformation(formWithErrors))
      },
      profile => {
        val oldProfile = Profile.findUserByLogin(profile.login).get
        Profile.updateUser(Profile(profile.id, profile.login, oldProfile.password, profile.name, profile.gender, profile.birthDay))
        Redirect(routes.Application.index()).withSession(LOGIN_KEY -> profile.login)
      }
    )
  }

  def myAccount = Action {
    implicit request =>
      Ok(html.basicInformation(profileUpdateForm))
  }

  def signout = Action {
    implicit request =>
      Redirect(routes.Application.index()).withNewSession
  }


}