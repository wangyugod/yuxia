package controllers

import play.api.mvc._
import play.api.data.Form
import play.api.data.Forms._
import models._
import views.html
import play.api.i18n.Messages
import java.sql.Timestamp
import play.api.Logger
import util.{AppHelper, DBHelper}
import scala.Some
import java.util.Date
import play.api.libs.json.{JsString, JsObject, JsArray}
import java.io.File

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

object InternalManagement extends Controller with InternalUsers with InternalMgtSecured {

  private val log = Logger(this.getClass())

  val loginForm: Form[(String, String)] = Form(
    tuple(
      "login" -> nonEmptyText,
      "password" -> nonEmptyText
    )
  )

  val interUserUpdateForm: Form[InternalUser] = Form(
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
    )((id, name, detail, parentAreaId) => Area(id.getOrElse(LocalIdGenerator.generateAreaId()), name, detail, parentAreaId, new Timestamp(new java.util.Date().getTime())))(
        (area: Area) => Some(Some(area.id), area.name, area.detail, area.parentAreaId))

  )

  def login = Action {
    implicit request => {
      Ok(html.admin.login(loginForm))
    }
  }

  def logout = Action {
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
            BadRequest(html.admin.login(loginForm.withError("login.failed", Messages("login.failed.msg"))))
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

  def areaList = isAuthenticated {
    implicit request => {
      val areaList = Area.all()
      if (log.isDebugEnabled)
        log.debug("area list is " + areaList)
      Ok(html.admin.areamgt(areaList, areaForm))
    }
  }

  def update = Action {
    Ok
  }

  def deleteArea(id: String) = isAuthenticated {
    implicit request => {
      Area.delete(id)
      Ok
    }
  }

  def modifyArea = isAuthenticated {
    implicit request => {
      areaForm.bindFromRequest().fold(
        formWithErrors => {
          if (log.isDebugEnabled)
            log.debug("error form is :\n" + formWithErrors)
          BadRequest(html.admin.areamgt(Area.all(), formWithErrors))
        },
        area => {
          if (log.isDebugEnabled)
            log.debug("preapare to update/create area : " + area)
          Area.saveOrUpdate(area)
          Redirect(routes.InternalManagement.areaList())
        }
      )
    }
  }

  def promoBannerList = isAuthenticated {
    implicit request =>
      Ok(html.admin.promobanner(PromotionBanner.promotionBannerList))
  }

  val pbForm = Form(
    mapping(
      "id" -> optional(text),
      "name" -> nonEmptyText
    )((id, name) => PromotionBanner(id.getOrElse(LocalIdGenerator.generatePbId()), name))
      ((pb: PromotionBanner) => Some(Some(pb.id), pb.name))
  )

  val pbItemForm = Form(
    tuple(
      "id" -> optional(text),
      "isProductPromo" -> optional(text),
      "description" -> optional(text),
      "imageUrl" -> optional(text),
      "link" -> optional(text),
      "promotionBannerId" -> nonEmptyText,
      "productIds" -> optional(text)
    )
  )

  def createPromoBanner = isAuthenticated {
    implicit request =>
      pbForm.bindFromRequest().fold(
        formWithErrors => {
          BadRequest(html.admin.promobanner(PromotionBanner.promotionBannerList))
        },
        pb => {
          DBHelper.database.withTransaction {
            implicit session =>
              PromotionBanner.createOrUpdatePromotionBanner(pb)
          }
          Redirect(routes.InternalManagement.promoBannerList())
        }
      )
  }

  def createOrUpdatePromoBannerItem = Action(parse.multipartFormData) {
    implicit request =>
      pbItemForm.bindFromRequest().fold(
        formWithErrors => {
          log.debug("bad form:" + formWithErrors)
          BadRequest(html.admin.promobanner(PromotionBanner.promotionBannerList))
        },
        pbItem => {
          DBHelper.database.withTransaction {
            implicit session =>
              pbItem._2 match {
                case Some("true") =>
                  if (pbItem._7.isDefined) {
                    pbItem._7.get.split(",").foreach(
                      (id: String) => {
                        val product = Product.findById(id).get
                        PromotionBanner.createOrUpdatePBItem(PromotionBannerItem(LocalIdGenerator.generatePbiId(), Some(product.name), AppHelper.productImage(product).url, Some(routes.Browse.productDetail(id).url), pbItem._6, Some(id), new Timestamp(new Date().getTime)))
                      }
                    )
                  }
                case _ =>
                  val pbiId = pbItem._1.getOrElse(LocalIdGenerator.generatePbiId())
                  request.body.file("image").map {
                    file => {
                      val dir: String = AppHelper.promoImageDir
                      file.ref.moveTo(new File(dir + pbiId + ".jpg"), true)
                    }
                  }
                  PromotionBanner.createOrUpdatePBItem(PromotionBannerItem(pbItem._1.getOrElse(LocalIdGenerator.generatePbiId()), pbItem._3, AppHelper.promoImage(pbiId + ".jpg").url, pbItem._5, pbItem._6, None, new Timestamp(new Date().getTime)))
              }
          }
          Redirect(routes.InternalManagement.promoBannerList())
        }
      )
  }

  def deletePromoBannerItem(pbiId: String) = isAuthenticated{
    implicit request =>
      DBHelper.database.withTransaction{
        implicit session =>
          PromotionBanner.deletePromotionBannerItem(pbiId)
      }
      Redirect(routes.InternalManagement.promoBannerList())
  }

  def listProducts = isAuthenticated {
    implicit request =>
      val products = DBHelper.database.withSession {
        implicit session =>
          Product.listAll.map(
            product => JsObject(List("id" -> JsString(product.id), "name" -> JsString(product.name), "url" -> JsString(AppHelper.productImage(product).url)))
          )
      }
      Ok(JsArray(products))
  }


}
