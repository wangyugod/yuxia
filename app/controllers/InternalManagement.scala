package controllers

import play.api.mvc._
import play.api.data.Form
import play.api.data.Forms._
import scala.Some
import models.{Area, InternalUser}
import views.html
import play.api.i18n.Messages
import java.sql.Timestamp
import util.IdGenerator
import play.api.Logger

/**
 * Created with IntelliJ IDEA.
 * User: simonwang
 * Date: 9/8/13
 * Time: 11:30 AM
 * To change this template use File | Settings | File Templates.
 */

trait InternalUsers {
  val LOGIN_KEY = "internal_login"
  val ID = "internal_id"
  val NAME = "internal_name"
  implicit def toMerchant(implicit session: Session): Option[InternalUser] = {
    session.get(LOGIN_KEY) match {
      case Some(login) => Some(InternalUser(session.get(ID).get, session.get(LOGIN_KEY).get, "", session.get(NAME).get))
      case _ => None
    }
  }
}
object InternalManagement extends Controller with InternalUsers with InternalMgtSecured{

  private val log = Logger(this.getClass())

  val loginForm: Form[(String, String)] = Form(
    tuple(
      "login" -> nonEmptyText,
      "password" -> nonEmptyText
    )
  )

  val interUserUpdateForm:Form[InternalUser] = Form(
    mapping(
      "id" -> nonEmptyText,
      "login" -> nonEmptyText,
      "name" -> nonEmptyText
    )((id, login, name) => InternalUser(id, login, "", name))
      ((iu: InternalUser) => Some(iu.id, iu.login, iu.name))
  )

  val areaForm: Form[Area] = Form(
    mapping(
      "id" -> optional(text),
      "name" -> nonEmptyText,
      "detail" -> nonEmptyText,
      "parentAreaId" -> optional(text)
    )((id, name, detail, parentAreaId) => Area(id.getOrElse(IdGenerator.generateAreaId()), name, detail, parentAreaId, new Timestamp(new java.util.Date().getTime())))(
      (area: Area) => Some(Some(area.id), area.name, area.detail, area.parentAreaId))

  )

  def login = Action{
    implicit request => {
      Ok(html.admin.login(loginForm))
    }
  }

  def logout = Action{
    implicit request =>
      Redirect(routes.InternalManagement.login()).withNewSession
  }

  def authenticate = Action {
    implicit request =>
      loginForm.bindFromRequest.fold(
        formWithErrors => {
          BadRequest(html.admin.login(formWithErrors))
        },
        user => {
          val foundUser = InternalUser.authenticateUser(user._1, user._2)
          if (foundUser.isDefined) {
            Redirect(routes.InternalManagement.home()).withSession(LOGIN_KEY -> foundUser.get.login, NAME -> foundUser.get.name, ID -> foundUser.get.id)
          } else {
            BadRequest(html.login(loginForm.withError("login.failed", Messages("login.failed.msg"))))
          }
        }
      )
  }

  def home = isAuthenticated {
    implicit request => {
      val userId = request.session.get(ID).get
      Ok(html.admin.adminacctinfo(interUserUpdateForm.fill(InternalUser.findById(userId).get)))
    }
  }

  def areaList = isAuthenticated{
    implicit request => {
      val areaList = Area.all()
      if(log.isDebugEnabled)
        log.debug("area list is " + areaList)
      Ok(html.admin.areamgt(areaList, areaForm))
    }
  }

  def update = Action{
    Ok
  }

  def modifyArea = Action{
    implicit request => {
      areaForm.bindFromRequest().fold(
        formWithErrors => {
          if(log.isDebugEnabled)
            log.debug("error form is :\n" + formWithErrors)
          BadRequest(html.admin.areamgt(Area.all(), formWithErrors))
        },
        area => {
          if(log.isDebugEnabled)
            log.debug("preapare to update/create area : " + area)
          Area.saveOrUpdate(area)
          Redirect(routes.InternalManagement.areaList())
        }
      )
    }
  }

}
