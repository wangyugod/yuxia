package models

import java.sql.Date
import helper.DBHelper
import scala.Predef._
import play.api.db.DB
import play.api.Play.current
import slick.session.Database
import Database.threadLocalSession
import scala.slick.driver.H2Driver.simple._

/**
 * Created with IntelliJ IDEA.
 * User: simonwang
 * Date: 6/26/13
 * Time: 4:47 PM
 * To change this template use File | Settings | File Templates.
 */
case class Category(id: String, name: String, description: String, longDescription: String) {
  def childCategories: Seq[Category] = DBHelper.database.withSession {
    val childCatQuery = for (cc <- CategoryCategories if cc.parentCatId === id) yield cc.childCatId
    Query(Categories).where(_.id inSetBind childCatQuery.list()).list()
  }

  def parentCategory = DBHelper.database.withSession {
    val parentCategoryQuery = for (cc <- CategoryCategories if cc.childCatId === id) yield cc.parentCatId
    parentCategoryQuery.firstOption match {
      case Some(catId) => Query(Categories).where(_.id === catId).firstOption
      case None => None
    }
  }

  def isRoot = parentCategory.isEmpty
}

case class Product(id: String, name: String, description: String, longDescription: String, startDate: Date, endDate: Date, merchantId: String, imageUrl: String) {
  def categories: Seq[String] = DBHelper.database.withSession {
    val categories = for (pc <- ProductCategories if (pc.productId === id))
    yield pc.categoryId
    categories.list()
  }
}

case class ProductCategory(productId: String, categoryId: String)

case class CategoryCategory(parentCatId: String, childCatId: String)

case class Sku(id: String, name: String, description: String, longDescription: String, size: Int, productId: String)

case class ProductSku(skuId: String, productId: String)

case class Media(id: String, name: String, description: String, url: String)


object Products extends Table[Product]("product") {
  def id = column[String]("id", O.PrimaryKey)

  def name = column[String]("name")

  def description = column[String]("description")

  def longDescription = column[String]("long_desc")

  def startDate = column[Date]("start_date")

  def endDate = column[Date]("end_date")

  def merchantId = column[String]("merchant_id")

  def imageUrl = column[String]("image_url")

  def * = id ~ name ~ description ~ longDescription ~ startDate ~ endDate ~ merchantId ~ imageUrl <>(
    (id, name, description, longDescription, startDate, endDate, merchantId, imageUrl) => Product(id, name, description, longDescription, startDate, endDate, merchantId, imageUrl),
    (p: Product) => Some(p.id, p.name, p.description, p.longDescription, p.startDate, p.endDate, p.merchantId, p.imageUrl)
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

  def * = id ~ name ~ description ~ longDescription <>(
    (id, name, description, longDescription) => Category(id, name, description, longDescription),
    (cat: Category) => Some(cat.id, cat.name, cat.description, cat.longDescription)
    )
}

object CategoryCategories extends Table[CategoryCategory]("category_category") {
  def parentCatId = column[String]("parent_cat_id")

  def childCatId = column[String]("child_cat_id")

  def * = parentCatId ~ childCatId <>(CategoryCategory, CategoryCategory.unapply(_))

  def pk = primaryKey("prod_cat_pk", (parentCatId, parentCatId))
}

object Product {
  def create(p: Product, categoryIds: Seq[String]) = DBHelper.database.withTransaction {
    Products.insert(p)
    for (catId <- categoryIds) {
      ProductCategories.insert(ProductCategory(p.id, catId))
    }
  }

  def update(p: Product, categoryIds: Seq[String]) = DBHelper.database.withTransaction {
    val existingCatId = for (pc <- ProductCategories if (pc.productId === p.id)) yield pc.categoryId
    val list: List[String] = existingCatId.list()
    println("existing " + list + " new " + categoryIds)
    if (list != categoryIds) {
      println("update categories")
      Query(ProductCategories).where(_.productId === p.id).delete
      for (catId <- categoryIds) {
        ProductCategories.insert(ProductCategory(p.id, catId))
      }
    }
    Products.where(_.id === p.id).update(p)
  }


  def findByMerchantId(merchantId: String) = DBHelper.database.withSession {
    Query(Products).where(_.merchantId === merchantId).list()
  }

  def findById(id: String) = DBHelper.database.withSession {
    Query(Products).where(_.id === id).firstOption
  }

  def delete(id: String) = DBHelper.database.withTransaction {
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

}
