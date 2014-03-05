package models

import java.sql.{Timestamp, Date}
import util.DBHelper
import scala.Predef._
import play.api.db.DB
import play.api.Play.current
import slick.session.Database
import Database.threadLocalSession
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
    val childCatQuery = for (cc <- CategoryCategories if cc.parentCatId === id) yield cc.childCatId
    Query(Categories).where(_.id inSetBind childCatQuery.list()).list()
  }

  def parentCategory = DBHelper.database.withSession {
    val parentCategoryQuery = for (cc <- CategoryCategories if cc.childCatId === id) yield cc.parentCatId
    val parentCatOpt: Option[String] = parentCategoryQuery.firstOption
    parentCatOpt match {
      case Some(catId) => Query(Categories).where(_.id === catId).firstOption
      case None => None
    }
  }

  def isRoot = parentCategory.isEmpty
}

case class Product(id: String, name: String, description: String, longDescription: String, startDate: Option[Date], endDate: Option[Date], merchantId: String, imageUrl: String, lastUpdatedTime: Timestamp) {
  lazy val childSkus = getChildSkus
  lazy val categories = getCategories
  lazy val priceRange = getPriceRange

  def getCategories: Seq[Category] = DBHelper.database.withSession {
    val categories = for (pc <- ProductCategories if (pc.productId === id))
    yield pc.categoryId
    categories.list() match {
      case Nil => Nil
      case list => Query(Categories).where(_.id inSetBind (list)).list()
    }
  }

  def getChildSkus: Seq[Sku] = DBHelper.database.withSession {
    Query(Skus).where(_.parentProduct === id).list()
  }

  def getPriceRange: (BigDecimal, BigDecimal) = DBHelper.database.withSession {
    val sortedSkus = childSkus.sortWith((sku1, sku2) => {
      sku1.price < sku2.price
    })
    println("sorted sku is " + sortedSkus)
    (sortedSkus.head.price, sortedSkus.reverse.head.price)
  }
}

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


object Products extends Table[Product]("product") {
  def id = column[String]("id", O.PrimaryKey)

  def name = column[String]("name")

  def description = column[String]("description")

  def longDescription = column[String]("long_desc")

  def startDate = column[Option[Date]]("start_date")

  def endDate = column[Option[Date]]("end_date")

  def merchantId = column[String]("merchant_id")

  def imageUrl = column[String]("image_url")

  def lastUpdatedTime = column[Timestamp]("last_updated_time")

  def * = id ~ name ~ description ~ longDescription ~ startDate ~ endDate ~ merchantId ~ imageUrl ~ lastUpdatedTime <>(
    (id, name, description, longDescription, startDate, endDate, merchantId, imageUrl, lastUpdatedTime) => Product(id, name, description, longDescription, startDate, endDate, merchantId, imageUrl, lastUpdatedTime),
    (p: Product) => Some(p.id, p.name, p.description, p.longDescription, p.startDate, p.endDate, p.merchantId, p.imageUrl, p.lastUpdatedTime)
    )
}

object ProductCategories extends Table[ProductCategory]("product_category") {
  def productId = column[String]("prod_id")

  def categoryId = column[String]("category_id")

  def * = productId ~ categoryId <>(ProductCategory, ProductCategory.unapply(_))

  def pk = primaryKey("prod_cat_pk", (productId, categoryId))
}

object Categories extends Table[Category]("category") {
  def id = column[String]("id", O.PrimaryKey)

  def name = column[String]("name")

  def description = column[String]("description")

  def longDescription = column[String]("long_desc")

  def isTopNav = column[Boolean]("top_nav_flag")

  def * = id ~ name ~ description ~ longDescription ~ isTopNav <> (
    (id, name, description, longDescription, isTopNav) => Category(id, name, description, longDescription, isTopNav),
    (cat: Category) => Some(cat.id, cat.name, cat.description, cat.longDescription, cat.isTopNav)
    )
}

object Skus extends Table[Sku]("sku") {
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

  def * = id ~ name ~ description ~ skuType ~ parentProduct ~ listPrice ~ salePrice ~ saleStartDate ~ saleEndDate ~ lastUpdatedTime <>(
    (id, name, description, skuType, parentProduct, listPrice, salePrice, saleStartDate, saleEndDate, lastUpdatedTime) => Sku(id, name, description, skuType, parentProduct, listPrice, salePrice, saleStartDate, saleEndDate, lastUpdatedTime),
    (sku: Sku) => Some(sku.id, sku.name, sku.description, sku.skuType, sku.productId, sku.listPrice, sku.salePrice, sku.saleStartDate, sku.saleEndDate, sku.lastUpdatedTime)
    )
}

object CategoryCategories extends Table[CategoryCategory]("category_category") {
  def parentCatId = column[String]("parent_cat_id")

  def childCatId = column[String]("child_cat_id")

  def * = parentCatId ~ childCatId <>(CategoryCategory, CategoryCategory.unapply(_))

  def pk = primaryKey("cat_cat_pk", (parentCatId, childCatId))
}

object Product {
  def create(p: Product, categoryIds: Seq[String], childSkus: Seq[Sku]) = DBHelper.database.withTransaction {
    Products.insert(p)
    for (catId <- categoryIds) {
      ProductCategories.insert(ProductCategory(p.id, catId))
    }
    childSkus.foreach(Skus.insert(_))
  }

  def update(p: Product, categoryIds: Seq[String], childSkus: Seq[Sku]) = DBHelper.database.withTransaction {
    val existingCatId = for (pc <- ProductCategories if (pc.productId === p.id)) yield pc.categoryId
    val list: List[String] = existingCatId.list()
    //check existing categories equals new
    if (list != categoryIds) {
      println("updating categories for product")
      Query(ProductCategories).where(_.productId === p.id).delete
      for (catId <- categoryIds) {
        ProductCategories.insert(ProductCategory(p.id, catId))
      }
    }
    val existingProd = findById(p.id).get
    //check if poperties changes, if not change don't update
    if (p != existingProd) {
      println("update product")
      Products.where(_.id === p.id).update(p)
    }

    println("childSkus is " + childSkus)
    if (childSkus != existingProd.childSkus) {
      println("updating skus for product")
      Query(Skus).where(_.parentProduct === p.id).delete
      for (sku <- childSkus) {
        Skus.insert(sku)
      }
    }
  }


  def findByMerchantId(merchantId: String) = DBHelper.database.withSession {
    Query(Products).where(_.merchantId === merchantId).list()
  }

  def findById(id: String) = DBHelper.database.withSession {
    Query(Products).where(_.id === id).firstOption
  }

  def findSkuById(id: String) = DBHelper.database.withSession{
    Query(Skus).where(_.id === id).firstOption
  }

  def delete(id: String) = DBHelper.database.withTransaction {
    Query(ProductCategories).where(_.productId === id).delete
    Query(Skus).where(_.parentProduct === id).delete
    Products.where(_.id === id).delete
  }
}

object Category {
  def create(category: Category, parentCatId: Option[String]) = DBHelper.database.withTransaction {
    Categories.insert(category)
    if (parentCatId.isDefined) {
      CategoryCategories.insert(CategoryCategory(parentCatId.get, category.id))
    }
  }

  def allCategories(): Seq[Category] = DBHelper.database.withSession {
    Query(Categories).list()
  }

  def rootCategories(): Seq[Category] = DBHelper.database.withSession {
    allCategories().filter(_.isRoot)
  }

  def childCategories(catId: String): Seq[Category] = DBHelper.database.withSession {
    findById(catId) match {
      case Some(category) => category.childCategories
      case None => Nil
    }
  }

  def findByIds(ids: Seq[String]): Seq[Category] = DBHelper.database.withSession {
    Query(Categories).where(_.id inSetBind ids).list()
  }

  def findById(id: String): Option[Category] = DBHelper.database.withSession {
    Query(Categories).where(_.id === id).firstOption
  }

  lazy val topNavCategories: Seq[Category] = DBHelper.database.withSession{
    Query(Categories).where(_.isTopNav === true).list()
  }
}
