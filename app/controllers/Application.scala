package controllers

import play.api.mvc._
import models._
import play.api.i18n.Messages
import views._
import util.DBHelper
import play.api.Play.current
import play.api.cache.Cache
import play.api.Logger

object Application extends Controller with Users {
  private val log = Logger(this.getClass)

  def index = Action {
    implicit request => {
      val users = Profile.findAllUsers()
      Ok(html.index(Messages("site.name"), users))
    }
  }

  def pageNotFound = Action {
    implicit request => {
      BadRequest(html.pageNotFound())
    }
  }

  def findPromotionBannerByName(pbName: String) = Cache.get(pbName) match {
    case Some(pbList: List[PromotionBannerItem]) =>
      if (log.isDebugEnabled)
        log.debug(s"get pbList $pbName from cache")
      pbList
    case _ =>
      if (log.isDebugEnabled)
        log.debug(s"set pbItems $pbName to cache")
      DBHelper.database.withSession {
        implicit session =>
          val pbList = PromotionBanner.findPromotionBannerItemsByName(pbName)
          Cache.set(pbName, pbList)
          pbList
      }
  }
}