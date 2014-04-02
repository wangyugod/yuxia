package models

import scala.slick.driver.MySQLDriver.simple._
import util.DBHelper

/**
 * Created by thinkpad-pc on 14-4-1.
 */
case class PromotionBanner(id: String, name: String) {
}

case class PromotionBannerItem(id: String, imageUrl: String, link: Option[String], promotionBannerId: String, porductId: Option[String])

class PromotionBannerRepo(tag: Tag) extends Table[PromotionBanner](tag, "promo_banner") {
  def id = column[String]("id", O.PrimaryKey)

  def name = column[String]("name")

  def * = (id, name) <>(PromotionBanner.tupled, PromotionBanner.unapply)
}

class PromotionBannerItemRepo(tag: Tag) extends Table[PromotionBannerItem](tag, "promo_banner_item") {
  def id = column[String]("id", O.PrimaryKey)

  def imageUrl = column[String]("image_url")

  def link = column[Option[String]]("link")

  def promoBannerId = column[String]("promo_banner_id")

  def productId = column[Option[String]]("product_id")

  def * = (id, imageUrl, link, promoBannerId, productId) <>(PromotionBannerItem.tupled, PromotionBannerItem.unapply)
}


object PromotionBanner extends ((String, String) => PromotionBanner) {
  val HOME_PAGE_SLIDER = "HomePageMainSlider"

  def findPromotionBannerItemsByName(name: String) = DBHelper.database.withSession {
    implicit session =>
      TableQuery[PromotionBannerRepo].filter(_.name === name).firstOption match {
        case Some(promoBanner) =>
          TableQuery[PromotionBannerItemRepo].filter(_.promoBannerId === promoBanner.id).list()
        case _ =>
          Nil
      }
  }


}


