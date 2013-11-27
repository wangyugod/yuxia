package models

import java.sql.Timestamp

import play.api.db.DB
import play.api.Play.current
import slick.session.Database
import Database.threadLocalSession
import scala.slick.driver.MySQLDriver.simple._

/**
 * Created with IntelliJ IDEA.
 * User: simonwang
 * Date: 10/30/13
 * Time: 11:08 PM
 * To change this template use File | Settings | File Templates.
 */
case class Order(id: String, profileId: String, state: Int, priceId: String, createdTime: Timestamp, modifiedTime: Timestamp) {

}

case class CommerceItem(id: String, skuId: String, orderId: String, priceId: String, createdTime: Timestamp)

case class PriceInfo(id: String, listPrice: BigDecimal, salePrice: BigDecimal, promoDescription: Option[String])

case class PaymentGroup(id: String, name: String, amount: BigDecimal, orderId: String)

case class ShippingGroup(id:String, addressId: String, shippingMethod: String, orderId: String)

object OrderRepo extends Table[Order]("order"){
  def id = column[String]("id", O.PrimaryKey)
  def profileId = column[String]("profile_id")
  def state = column[Int]("state")
  def priceId = column[String]("price_id")
  def createdTime = column[Timestamp]("created_time")
  def modifiedTime = column[Timestamp]("modified_time")
  def * = id ~ profileId ~ state ~ priceId ~ createdTime ~ modifiedTime <> (
    (id, profileId, state, priceId, createdTime, modifiedTime) => Order(id, profileId, state, priceId, createdTime, modifiedTime),
    (order: Order) => Some(order.id, order.profileId, order.state, order.priceId, order.createdTime, order.modifiedTime)
    )
}

object CommerceItemRepo extends Table[CommerceItem]("commerce_item"){
  def id = column[String]("id", O.PrimaryKey)
  def skuId = column[String]("sku_id")
  def orderId = column[String]("order_id")
  def priceId = column[String]("price_id")
  def createdTime = column[Timestamp]("created_time")
  def * = id ~ skuId ~ orderId ~ priceId ~ createdTime <> (
    (id, skuId, orderId, priceId, createdTime) => CommerceItem(id, skuId, orderId, priceId, createdTime),
    (ci: CommerceItem) => Some(ci.id, ci.skuId, ci.orderId, ci.priceId, ci.createdTime)
    )
}

object PriceInfoRepo extends Table[PriceInfo]("price_info"){
  def id = column[String]("id", O.PrimaryKey)
  def listPrice = column[BigDecimal]("list_price")
  def salePrice = column[BigDecimal]("sale_price")
  def promoDescription = column[Option[String]]("promo_desc")
  def * = id ~ listPrice ~ salePrice ~ promoDescription <>(
    (id, listPrice, salePrice, promoDescription) => PriceInfo(id, listPrice, salePrice, promoDescription),
    (priceInfo: PriceInfo) => Some(priceInfo.id, priceInfo.listPrice, priceInfo.salePrice, priceInfo.promoDescription)
    )
}

object PaymentGroupRepo extends Table[PaymentGroup]("payment_group"){
  def id = column[String]("id", O.PrimaryKey)
  def name = column[String]("name")
  def amount = column[BigDecimal]("amount")
  def orderId = column[String]("order_id")
  def * = id ~ name ~ amount ~ orderId <>(PaymentGroup, PaymentGroup.unapply _)
}

object ShippingGroupRepo extends Table[ShippingGroup]("shipping_group"){
  def id = column[String]("id", O.PrimaryKey)
  def addressId = column[String]("address_id")
  def shippingMethod = column[String]("shipping_method")
  def orderId = column[String]("order_id")
  def * = id ~ addressId~ shippingMethod~ orderId <>(ShippingGroup, ShippingGroup.unapply _)
}




object OrderState{
  val INITIAL: Int = 0
  val SUBMITTED: Int = 1
  val COMPLETE: Int = 2
  val CANCELLED: Int = 3
}

object Order{

  def priceOrder(order: Order) = {


  }

  def addItem(order: Order, skuId: String, quantity: Int) = {

  }

  def newOrder(profileId: String) = {

  }




}
