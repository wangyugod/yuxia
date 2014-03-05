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
  val log = Logger(this.getClass)
  def commerceItems = DBHelper.database.withSession{
    val cis = Query(CommerceItemRepo).where(_.orderId === id).list()
    cis
  }

  def itemSize = (commerceItems.map(_.quantity):\0)(_ + _)

  def removeCommerceItem(itemId: String) = DBHelper.database.withTransaction{
    if(log.isDebugEnabled)
      log.debug(s"remove commerceItem with ID $itemId")
    Query(CommerceItemRepo).where(_.id === itemId).delete
    reprice
  }

  def reprice() = Order.priceOrder(this)
}

case class CommerceItem(id: String, skuId: String, orderId: String, priceId: String,quantity: Int, createdTime: Timestamp){
  lazy val sku = Product.findSkuById(skuId).get
  lazy val priceInfo = DBHelper.database.withSession{
    Query(PriceInfoRepo).where(_.id === priceId).first()
  }
  def totalActualPrice = priceInfo.actualPrice * quantity
  def totalListPrice = priceInfo.listPrice * quantity
}

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


  def findOrderById(orderId: String) = DBHelper.database.withSession{
    Query(OrderRepo).where(_.id === orderId).firstOption
  }

  /**
   * Price Order and persist order price information
   * @param order
   */
  def priceOrder(order: Order) = DBHelper.database.withTransaction{
    val actualPrice = (order.commerceItems.map(_.totalActualPrice):\(BigDecimal(0)))(_ + _)
    val listPrice = (order.commerceItems.map(_.totalListPrice):\(BigDecimal(0)))(_ + _)
    log.debug(s"totalActualPrice: $actualPrice, totalListPrice: $listPrice")
    order.priceId match {
      case Some(pid) => {
        Query(PriceInfoRepo).where(_.id === pid).update(PriceInfo(pid, listPrice, actualPrice, None))
      }
      case _ => {
        val priceInfoId = IdGenerator.generatePriceInfoId()
        PriceInfoRepo.insert(PriceInfo(priceInfoId, listPrice, actualPrice, None))
        Query(OrderRepo).where(_.id === order.id).update(Order(order.id, order.profileId, order.state, Some(priceInfoId), order.createdTime, new Timestamp(new Date().getTime)))
      }
    }
    log.debug("price order done!")
  }

  /**
   * Add SKU to order, if order already contain this SKU, merge it, otherwise add new commerceItem
   * @param profileId
   * @param orderId
   * @param skuId
   * @param quantity
   * @return
   */
  def addItem(profileId: String, orderId: Option[String], skuId: String, quantity: Int) = {
    val order = DBHelper.database.withTransaction{
      if(log.isDebugEnabled)
        log.debug(s"add new item with skuId $skuId, quantity $quantity")
      val order = orderId match {
        case Some(id) =>
          log.debug(s"Found existed order with id $orderId")
          Query(OrderRepo).where(_.id === orderId).first()
        case None =>
          log.debug(s"No order found, create new!")
          create(profileId)
      }
      //GET SKU Price
      val sku = Product.findSkuById(skuId) match {
        case Some(sku) => sku
        case None => throw new java.util.NoSuchElementException(s"SKU with id $skuId cannot be found")
      }
      val existedCi = order.commerceItems.filter(_.skuId == skuId).headOption
      existedCi match {
        case Some(ci) =>
          //Merge existed commerceItem
          log.debug(s"found existed commerce item")
          val commerceItem = CommerceItem(ci.id, ci.skuId, order.id, ci.priceInfo.id,quantity, new Timestamp(new Date().getTime))
          Query(CommerceItemRepo).where(_.id === ci.id).update(commerceItem)
        case _ =>
          //Add new commerceItem
          log.debug(s"No existed item found, add new!")
          val priceInfo = PriceInfo(IdGenerator.generatePriceInfoId(), sku.listPrice, sku.price, None)
          val commerceItem = CommerceItem(IdGenerator.generateCommerceItemId(), skuId, order.id, priceInfo.id,quantity,  new Timestamp(new Date().getTime))
          PriceInfoRepo.insert(priceInfo)
          CommerceItemRepo.insert(commerceItem)
      }
      order
    }
    order.reprice
    order
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
