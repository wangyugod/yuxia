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

case class Product(id: String, name: String, description: String, longDescription: String, startDate: Date, endDate: Date, merchantId: String, template: String)

case class ProductCategory(productId: String, categoryId: String)

case class Sku(id: String, name: String, description: String, longDescription: String, size: Int, productId: String)

case class ProductSku(skuId: String, productId: String)

case class Media(id: String, name: String, description: String, url: String)


object Products extends Table[Product]("product") {
  def id = column[String]("id")
  def name = column[String]("name")
  def description = column[String]("description")
  def longDescription = column[String]("long_desc")
  def startDate = column[Date]("start_date")
  def endDate = column[Date]("end_date")
  def merchantId = column[String]("merchant_id")


}
