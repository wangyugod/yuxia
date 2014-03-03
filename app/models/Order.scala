package models

import java.sql.Timestamp

import play.api.db.DB
import play.api.Play.current
import slick.session.Database
import Database.threadLocalSession
import scala.slick.driver.MySQLDriver.simple._
import util.{DBHelper, IdGenerator}
import java.util.Date
import play.api.Logger

/**
 * Created with IntelliJ IDEA.
 * User: simonwang
 * Date: 10/30/13
 * Time: 11:08 PM
 * To change this template use File | Settings | File Templates.
 */
case class Order(id: String, profileId: String, state: Int, priceId: Option[String], createdTime: Timestamp, modifiedTime: Timestamp) {
  lazy val commerceItems = DBHelper.database.withSession{
    Query(CommerceItemRepo).where(_.orderId === id).list()
  }

  lazy val itemSize = (commerceItems.map(_.quantity):\0)(_ + _)
}

case class CommerceItem(id: String, skuId: String, orderId: String, priceId: String,quantity: Int, createdTime: Timestamp)

case class PriceInfo(id: String, listPrice: BigDecimal, actualPrice: BigDecimal, promoDescription: Option[String])

case class PaymentGroup(id: String, name: String, amount: BigDecimal, orderId: String)

case class ShippingGroup(id:String, addressId: String, shippingMethod: String, orderId: String)

object OrderRepo extends Table[Order]("order"){
  def id = column[String]("id", O.PrimaryKey)
  def profileId = column[String]("profile_id")
  def state = column[Int]("state")
  def priceId = column[Option[String]]("price_id")
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
  def quantity = column[Int]("quantity")
  def createdTime = column[Timestamp]("created_time")
  def * = id ~ skuId ~ orderId ~ priceId ~ quantity ~ createdTime <> (
    (id, skuId, orderId, priceId, quantity, createdTime) => CommerceItem(id, skuId, orderId, priceId,quantity, createdTime),
    (ci: CommerceItem) => Some(ci.id, ci.skuId, ci.orderId, ci.priceId, ci.quantity, ci.createdTime)
    )
}

object PriceInfoRepo extends Table[PriceInfo]("price_info"){
  def id = column[String]("id", O.PrimaryKey)
  def listPrice = column[BigDecimal]("list_price")
  def actualPrice = column[BigDecimal]("actual_price")
  def promoDescription = column[Option[String]]("promo_desc")
  def * = id ~ listPrice ~ actualPrice ~ promoDescription <>(
    (id, listPrice, actualPrice, promoDescription) => PriceInfo(id, listPrice, actualPrice, promoDescription),
    (priceInfo: PriceInfo) => Some(priceInfo.id, priceInfo.listPrice, priceInfo.actualPrice, priceInfo.promoDescription)
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

  val log = Logger(this.getClass)

  def priceOrder(order: Order) = {


  }

  /**
   * Add SKU to order
   * @param profileId
   * @param orderId
   * @param skuId
   * @param quantity
   * @return
   */
  def addItem(profileId: String, orderId: Option[String], skuId: String, quantity: Int) = DBHelper.database.withSession{
    val order = orderId match {
      case Some(id) =>
        Query(OrderRepo).where(_.id === orderId).first()
      case None =>
        create(profileId)
    }
    //GET SKU Price
    val sku = Product.findSkuById(skuId) match {
      case Some(sku) => sku
      case None => throw new java.util.NoSuchElementException(s"SKU with id $skuId cannot be found")
    }
    val priceInfo = PriceInfo(IdGenerator.generatePriceInfoId(), sku.listPrice, sku.getPrice, None)
    val ci = CommerceItem(IdGenerator.generateCommerceItemId(), skuId, order.id, priceInfo.id,quantity,  new Timestamp(new Date().getTime))
    DBHelper.database.withTransaction{
      PriceInfoRepo.insert(priceInfo)
      CommerceItemRepo.insert(ci)
    }
    log.debug(s"SKU $skuId is added to order successfully")
  }

  def create(profileId: String) = {
    val order = Order(IdGenerator.generateOrderId(), profileId, OrderState.INITIAL,None, new Timestamp(new Date().getTime()), new Timestamp(new Date().getTime()) )
    DBHelper.database.withTransaction{
      OrderRepo.insert(order)
    }
    log.debug(s"order is created with id $order.id")
    order
  }

}
