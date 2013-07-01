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
case class Category(id: String, name: String, description: String, longDescription: String)

case class Product(id: String, name: String, description: String, longDescription: String, startDate: Date, endDate: Date, merchantId: String)

case class ProductCategory(productId: String, categoryId: String)

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

  def * = id ~ name ~ description ~ longDescription ~ startDate ~ endDate ~ merchantId <>(
    (id, name, description, longDescription, startDate, endDate, merchantId) => Product(id, name, description, longDescription, startDate, endDate, merchantId),
    (p: Product) => Some(p.id, p.name, p.description, p.longDescription, p.startDate, p.endDate, p.merchantId)
  )
}

object ProductCategories extends Table[ProductCategory]("product_category"){
  def productId = column[String]("prod_id")
  def categoryId = column[String]("category_id")
  def * = productId ~ categoryId <> (ProductCategory, ProductCategory.unapply(_))
  def pk = primaryKey("prod_cat_pk", (productId, categoryId))
}

object Product{
  def create(p:Product, categoryIds:Seq[String]) = DBHelper.database.withTransaction{
    Products.insert(p)
    for(catId <- categoryIds){
      ProductCategories.insert(ProductCategory(p.id, catId))
    }
  }


}
