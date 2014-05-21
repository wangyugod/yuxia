package models

import java.sql.{Timestamp, Date}
import util.DBHelper
import scala.Predef._
import play.api.db.DB
import play.api.Play.current
import scala.slick.driver.MySQLDriver.simple._
import java.util.Calendar

/**
 * Created with IntelliJ IDEA.
 * User: simonwang
 * Date: 6/26/13
 * Time: 4:47 PM
 * To change this template use File | Settings | File Templates.
 */
case class Category(id: String, name: String, description: String, longDescription: String, isTopNav: Boolean) {
  def childCategories: Seq[Category] = DBHelper.database.withSession {
    implicit session =>
      val catcat = TableQuery[CategoryCategories]
      //      val childCatQuery = for (cc <- catcat if cc.parentCatId === id) yield cc.childCatId
      val childCatIds = catcat.filter(_.parentCatId === id).list().map(_.childCatId)
      TableQuery[Categories].where(_.id inSetBind childCatIds).list()
  }

  def parentCategory = DBHelper.database.withSession {
    implicit session =>
      val catcat = TableQuery[CategoryCategories]
      val parentCategoryQuery = for (cc <- catcat if cc.childCatId === id) yield cc.parentCatId
      val parentCatOpt: Option[String] = parentCategoryQuery.firstOption
      parentCatOpt match {
        case Some(catId) => TableQuery[Categories].where(_.id === catId).firstOption
        case None => None
      }
  }

  def isRoot = parentCategory.isEmpty
}

case class Product(id: String, name: String, description: String, longDescription: String, startDate: Option[Date], endDate: Option[Date], merchantId: String, imageUrl: String, lastUpdatedTime: Timestamp) {
  lazy val childSkus = getChildSkus
  lazy val categories = getCategories
  lazy val priceRange = getPriceRange
  lazy val listPriceRange = {
    val sortedSkus = childSkus.sortWith((sku1, sku2) => {
      sku1.listPrice < sku2.listPrice
    })
    (sortedSkus.head.listPrice, sortedSkus.reverse.head.listPrice)
  }

  lazy val salesVolume = DBHelper.database.withSession {
    implicit session =>
      Product.getSalesVolume(this.id) match {
        case Some(salesVolumen) =>
          salesVolumen.volume
        case _ =>
          0
      }
  }

  def getCategories: Seq[Category] = DBHelper.database.withSession {
    implicit session =>
      val productCategory = TableQuery[ProductCategories]
      val categories = for (pc <- productCategory if (pc.productId === id)) yield pc.categoryId
      categories.list() match {
        case Nil => Nil
        case list => TableQuery[Categories].where(_.id inSetBind (list)).list()
      }
  }


  def getChildSkus: Seq[Sku] = DBHelper.database.withSession {
    implicit session =>
      TableQuery[Skus].where(_.parentProduct === id).list()
  }

  def getPriceRange: (BigDecimal, BigDecimal) = {
    val sortedSkus = childSkus.sortWith((sku1, sku2) => {
      sku1.price < sku2.price
    })
    (sortedSkus.head.price, sortedSkus.reverse.head.price)
  }
}

case class ProductSalesVolume(productId: String, volume: Int, lastModifiedTime: Timestamp)

case class ProductCategory(productId: String, categoryId: String)

case class CategoryCategory(parentCatId: String, childCatId: String)

case class Sku(id: String, name: String, description: Option[String], skuType: Option[String], productId: String, listPrice: BigDecimal, salePrice: Option[BigDecimal], saleStartDate: Option[Date], saleEndDate: Option[Date], lastUpdatedTime: Timestamp) {
  lazy val price = getPrice
  lazy val parentProduct = Product.findById(productId).get

  def getPrice: BigDecimal = {
    salePrice match {
      case Some(spr) if (spr < listPrice) => {
        (saleStartDate, saleEndDate) match {
          case (None, None) => spr
          case (None, Some(sed)) if (saleEndDate.get.getTime >= Calendar.getInstance().getTimeInMillis) => spr
          case (Some(ssd), None) if (ssd.getTime <= Calendar.getInstance().getTimeInMillis) => {
            spr
          }
          case (Some(ssd), Some(sed)) if (ssd.getTime <= Calendar.getInstance().getTimeInMillis && saleEndDate.get.getTime >= Calendar.getInstance().getTimeInMillis) => {
            spr
          }
          case _ => listPrice
        }
      }
      case _ => listPrice
    }
  }
}

case class ProductSku(skuId: String, productId: String)

case class Media(id: String, name: String, description: String, url: String)


class Products(tag: Tag) extends Table[Product](tag, "product") {
  def id = column[String]("id", O.PrimaryKey)

  def name = column[String]("name")

  def description = column[String]("description")

  def longDescription = column[String]("long_desc")

  def startDate = column[Option[Date]]("start_date")

  def endDate = column[Option[Date]]("end_date")

  def merchantId = column[String]("merchant_id")

  def imageUrl = column[String]("image_url")

  def lastUpdatedTime = column[Timestamp]("last_updated_time")

  def * = (id, name, description, longDescription, startDate, endDate, merchantId, imageUrl, lastUpdatedTime) <>(Product.tupled, Product.unapply)
}

class ProductCategories(tag: Tag) extends Table[ProductCategory](tag, "product_category") {
  def productId = column[String]("prod_id")

  def categoryId = column[String]("category_id")

  def * = (productId, categoryId) <>(ProductCategory.tupled, ProductCategory.unapply)

  def pk = primaryKey("prod_cat_pk", (productId, categoryId))
}

class Categories(tag: Tag) extends Table[Category](tag, "category") {
  def id = column[String]("id", O.PrimaryKey)

  def name = column[String]("name")

  def description = column[String]("description")

  def longDescription = column[String]("long_desc")

  def isTopNav = column[Boolean]("top_nav_flag")

  def * = (id, name, description, longDescription, isTopNav) <>(Category.tupled, Category.unapply)
}

class Skus(tag: Tag) extends Table[Sku](tag, "sku") {
  def id = column[String]("id", O.PrimaryKey)

  def name = column[String]("name")

  def description = column[Option[String]]("description")

  def parentProduct = column[String]("parent_product")

  def skuType = column[Option[String]]("sku_type")

  def listPrice = column[BigDecimal]("list_price")

  def salePrice = column[Option[BigDecimal]]("sale_price")

  def saleStartDate = column[Option[Date]]("sale_start_date")

  def saleEndDate = column[Option[Date]]("sale_end_date")

  def lastUpdatedTime = column[Timestamp]("last_updated_time")

  def * = (id, name, description, skuType, parentProduct, listPrice, salePrice, saleStartDate, saleEndDate, lastUpdatedTime) <>(Sku.tupled, Sku.unapply)
}

class ProductSalesVolumeRepo(tag: Tag) extends Table[ProductSalesVolume](tag, "product_sales_volume") {
  def productId = column[String]("product_id", O.PrimaryKey)

  def volume = column[Int]("volume")

  def lastModifiedTime = column[Timestamp]("last_modified_time")

  def * = (productId, volume, lastModifiedTime) <>(ProductSalesVolume.tupled, ProductSalesVolume.unapply)
}

class CategoryCategories(tag: Tag) extends Table[CategoryCategory](tag, "category_category") {
  def parentCatId = column[String]("parent_cat_id")

  def childCatId = column[String]("child_cat_id")

  def * = (parentCatId, childCatId) <>(CategoryCategory.tupled, CategoryCategory.unapply)

  def pk = primaryKey("cat_cat_pk", (parentCatId, childCatId))
}

object Product extends ((String, String, String, String, Option[Date], Option[Date], String, String, Timestamp) => Product) {
  private val productRepo = TableQuery[Products]

  def create(p: Product, categoryIds: Seq[String], childSkus: Seq[Sku]) = DBHelper.database.withTransaction {
    implicit session =>
      productRepo.insert(p)
      for (catId <- categoryIds) {
        TableQuery[ProductCategories].insert(ProductCategory(p.id, catId))
      }
      childSkus.foreach(TableQuery[Skus].insert(_))
  }

  def update(p: Product, categoryIds: Seq[String], childSkus: Seq[Sku]) = DBHelper.database.withTransaction {
    implicit session =>
      val productCategoryQuery = TableQuery[ProductCategories]
      val existingCatId = for (pc <- productCategoryQuery if (pc.productId === p.id)) yield pc.categoryId
      val list: List[String] = existingCatId.list()
      //check existing categories equals new
      if (list != categoryIds) {
        productCategoryQuery.where(_.productId === p.id).delete
        for (catId <- categoryIds) {
          productCategoryQuery.insert(ProductCategory(p.id, catId))
        }
      }
      val existingProd = findById(p.id).get
      //check if poperties changes, if not change don't update
      if (p != existingProd) {
        productRepo.where(_.id === p.id).update(p)
      }

      if (childSkus != existingProd.childSkus) {
        TableQuery[Skus].where(_.parentProduct === p.id).delete
        for (sku <- childSkus) {
          TableQuery[Skus].insert(sku)
        }
      }
  }

  def getSalesVolume(productId: String)(implicit session: Session) = {
    TableQuery[ProductSalesVolumeRepo].where(_.productId === productId).firstOption
  }

  def findByMerchantId(merchantId: String) = DBHelper.database.withSession {
    implicit session =>
      productRepo.where(_.merchantId === merchantId).list()
  }

  def findById(id: String) = DBHelper.database.withSession {
    implicit session =>
      productRepo.where(_.id === id).firstOption
  }

  def findSkuById(id: String)(implicit session: Session) = {
    TableQuery[Skus].where(_.id === id).firstOption
  }

  def delete(id: String) = DBHelper.database.withTransaction {
    implicit session =>
      TableQuery[ProductCategories].where(_.productId === id).delete
      TableQuery[Skus].where(_.parentProduct === id).delete
      productRepo.where(_.id === id).delete
  }

  def listAll(implicit session: Session) = {
    productRepo.list()
  }


  def fetchTopSellerProducts()(implicit session: Session) = {
    val salesRepo = TableQuery[ProductSalesVolumeRepo]
    val topVolume = salesRepo.sortBy(_.volume.desc).take(10).list()
    val productIds = for(vol <- topVolume) yield vol.productId
    TableQuery[Products].where(_.id inSetBind(productIds)).list()
  }
}

object Category extends ((String, String, String, String, Boolean) => Category) {
  def create(category: Category, parentCatId: Option[String]) = DBHelper.database.withTransaction {
    implicit session =>
      TableQuery[Categories].insert(category)
      if (parentCatId.isDefined) {
        TableQuery[CategoryCategories].insert(CategoryCategory(parentCatId.get, category.id))
      }
  }

  def allCategories(): Seq[Category] = DBHelper.database.withSession {
    implicit session =>
      TableQuery[Categories].list()
  }

  def rootCategories(): Seq[Category] = DBHelper.database.withSession {
    implicit session =>
      allCategories().filter(_.isRoot)
  }

  def childCategories(catId: String): Seq[Category] = DBHelper.database.withSession {
    implicit session =>
      findById(catId) match {
        case Some(category) => category.childCategories
        case None => Nil
      }
  }

  def findByIds(ids: Seq[String]): Seq[Category] = DBHelper.database.withSession {
    implicit session =>
      TableQuery[Categories].where(_.id inSetBind ids).list()
  }

  def findById(id: String): Option[Category] = DBHelper.database.withSession {
    implicit session =>
      TableQuery[Categories].where(_.id === id).firstOption
  }

  lazy val topNavCategories: Seq[Category] = DBHelper.database.withSession {
    implicit session =>
      TableQuery[Categories].where(_.isTopNav === true).list()
  }
}
