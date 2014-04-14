package models

import scala.slick.driver.MySQLDriver.simple._
import util.DBHelper
import java.sql.Timestamp

/**
 * Created by thinkpad-pc on 14-4-1.
 */
case class PromotionBanner(id: String, name: String) {
  val pbItems = DBHelper.database.withSession {
    implicit session =>
      PromotionBanner.findPromotionBannerItemByPbId(id)
  }
}

case class PromotionBannerItem(id: String,description: Option[String], imageUrl: String, link: Option[String], promotionBannerId: String, productId: Option[String], lastModifiedTime: Timestamp)

class PromotionBannerRepo(tag: Tag) extends Table[PromotionBanner](tag, "promo_banner") {
  def id = column[String]("id", O.PrimaryKey)

  def name = column[String]("name")

  def * = (id, name) <>(PromotionBanner.tupled, PromotionBanner.unapply)
}

class PromotionBannerItemRepo(tag: Tag) extends Table[PromotionBannerItem](tag, "promo_banner_item") {
  def id = column[String]("id", O.PrimaryKey)

  def description = column[Option[String]]("description")

  def imageUrl = column[String]("image_url")

  def link = column[Option[String]]("link")

  def promoBannerId = column[String]("promo_banner_id")

  def productId = column[Option[String]]("product_id")

  def lastModifiedTime = column[Timestamp]("last_modified_time")

  def * = (id, description, imageUrl, link, promoBannerId, productId, lastModifiedTime) <>(PromotionBannerItem.tupled, PromotionBannerItem.unapply)
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

  def findPromotionBannerItemByPbId(id: String)(implicit session: Session) = {
    TableQuery[PromotionBannerItemRepo].where(_.promoBannerId === id).list()
  }

  def promotionBannerList = DBHelper.database.withSession {
    implicit session =>
      TableQuery[PromotionBannerRepo].list()
  }

  def createOrUpdatePromotionBanner(pb: PromotionBanner)(implicit session: Session) = {
    val pbRepo = TableQuery[PromotionBannerRepo]
    pbRepo.where(_.id === pb.id).firstOption match {
      case Some(existedPb) =>
        if(existedPb.name != pb.name)
          pbRepo.where(_.id === pb.id).update(pb)
      case _ =>
        pbRepo.insert(pb)
    }
  }

  def createOrUpdatePBItem(pbItem: PromotionBannerItem)(implicit session: Session) = {
    val pbiRepo = TableQuery[PromotionBannerItemRepo]
    pbiRepo.where(_.id === pbItem.id).firstOption match{
      case Some(existedPbItem) =>
        pbiRepo.where(_.id === pbItem.id).update(pbItem)
      case _ =>
        pbiRepo.insert(pbItem)
    }
  }


}


