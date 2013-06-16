package controllers

import play.api.mvc.Controller
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import validation.Constraints
import views.html

import models._
import helper.AppHelper

/**
 * Created with IntelliJ IDEA.
 * User: simonwang
 * Date: 6/13/13
 * Time: 11:24 PM
 * To change this template use File | Settings | File Templates.
 */
object ProfileController extends Controller {

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
      ((id, login, passwords, name, gender, birthday) => Profile(id.getOrElse(""), login, passwords._1, name, gender, AppHelper.convertBirthdayFromText(birthday)))
      ((profile: Profile) => {
        Some((Some(profile.id), profile.login, (profile.password, ""), profile.name, profile.gender, AppHelper.convertBirthdayToText(profile.birthDay)))
      }) //verifying("User with the same loign already exists", profile => ProfileService.findUserByLogin(profile.login).isEmpty)
  )

  val loginForm: Form[(String, String)] = Form(
    tuple(
      "login" -> nonEmptyText.verifying(email.constraints.head),
      "password" -> text
    ) verifying("Invalid login or password", result => result match {
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
        user => Ok("Hello World").withSession("login" -> user._1)
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
      profile => Ok("Hello")
    )

  }


}
