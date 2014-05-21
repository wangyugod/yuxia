package controllers

import play.api.mvc._
import models._
import play.api.i18n.Messages
import views._
import util.DBHelper
import play.api.Play.current
import play.api.cache.{Cached, Cache}
import play.api.{Play, Logger}

object Application extends Controller with Users {
  private val log = Logger(this.getClass)

  def index = Cached("index", Play.current.configuration.getInt("cache.ttl").getOrElse(5)) {
    Action {
      implicit request => {
        Ok(html.index(Messages("site.name")))
      }
    }
  }

  def pageNotFound = Action {
    implicit request => {
      BadRequest(html.pageNotFound())
    }
  }

  def findPromotionBannerByName(pbName: String) = {
    if (Cache.get(pbName).isDefined) {
      Cache.getAs[List[PromotionBannerItem]](pbName).get
    } else {
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
}