package models

import java.sql.Timestamp

import play.api.db.DB
import play.api.Play.current
import play.api.Play.current

import scala.slick.driver.MySQLDriver.simple._
import util.{DBHelper, IdGenerator}
import java.util.Date
import play.api.Logger
import play.api.i18n.Messages

/**
 * Created with IntelliJ IDEA.
 * User: simonwang
 * Date: 10/30/13
 * Time: 11:08 PM
 * To change this template use File | Settings | File Templates.
 */
case class Order(id: String, profileId: String, state: Int, priceId: Option[String], createdTime: Timestamp, modifiedTime: Timestamp) {
  val log = Logger(this.getClass)

  lazy val commerceItems = DBHelper.database.withSession {
    implicit session =>
      Order.fetchCommerceItems(this)
  }
  lazy val itemSize = (commerceItems.map(_.quantity) :\ 0)(_ + _)
  lazy val priceInfo = DBHelper.database.withSession {
    implicit session =>
      TableQuery[PriceInfoRepo].where(_.id === priceId.get).first()
  }
  lazy val shippingAddress = DBHelper.database.withSession {
    implicit session =>
      Order.findOrderShippingGroup(id) match {
        case Some(sg) =>
          Address.findById(sg.addressId)
        case _ =>
          None
      }
  }

  def removeCommerceItem(itemId: String)(implicit session: Session) = {
    if (log.isDebugEnabled)
      log.debug(s"remove commerceItem with ID $itemId")
    TableQuery[CommerceItemRepo].where(_.id === itemId).delete
    Order.priceOrder(this)
  }

  def reprice() = DBHelper.database.withSession {
    implicit session =>
      Order.priceOrder(this)
  }

}

case class CommerceItem(id: String, skuId: String, orderId: String, priceId: String, quantity: Int, createdTime: Timestamp) {
  lazy val sku = Product.findSkuById(skuId).get
  lazy val priceInfo = DBHelper.database.withSession {
    implicit ss: Session =>
      TableQuery[PriceInfoRepo].where(_.id === priceId).first()
  }

  def totalActualPrice = priceInfo.actualPrice * quantity

  def totalListPrice = priceInfo.listPrice * quantity
}

case class PriceInfo(id: String, listPrice: BigDecimal, actualPrice: BigDecimal, promoDescription: Option[String])

case class PaymentGroup(id: String, name: String, amount: BigDecimal, orderId: String)

case class ShippingGroup(id: String, addressId: String, shippingMethod: String, orderId: String)

class OrderRepo(tag: Tag) extends Table[Order](tag, "order") {
  def id = column[String]("id", O.PrimaryKey)

  def profileId = column[String]("profile_id")

  def state = column[Int]("state")

  def priceId = column[Option[String]]("price_id")

  def createdTime = column[Timestamp]("created_time")

  def modifiedTime = column[Timestamp]("modified_time")

  def * = (id, profileId, state, priceId, createdTime, modifiedTime) <>(Order.tupled, Order.unapply)

}

class CommerceItemRepo(tag: Tag) extends Table[CommerceItem](tag, "commerce_item") {
  def id = column[String]("id", O.PrimaryKey)

  def skuId = column[String]("sku_id")

  def orderId = column[String]("order_id")

  def priceId = column[String]("price_id")

  def quantity = column[Int]("quantity")

  def createdTime = column[Timestamp]("created_time")

  def * = (id, skuId, orderId, priceId, quantity, createdTime) <>(CommerceItem.tupled, CommerceItem.unapply)

}

class PriceInfoRepo(tag: Tag) extends Table[PriceInfo](tag, "price_info") {
  def id = column[String]("id", O.PrimaryKey)

  def listPrice = column[BigDecimal]("list_price")

  def actualPrice = column[BigDecimal]("actual_price")

  def promoDescription = column[Option[String]]("promo_desc")

  def * = (id, listPrice, actualPrice, promoDescription) <>(PriceInfo.tupled, PriceInfo.unapply)
}

class PaymentGroupRepo(tag: Tag) extends Table[PaymentGroup](tag, "payment_group") {
  def id = column[String]("id", O.PrimaryKey)

  def name = column[String]("name")

  def amount = column[BigDecimal]("amount")

  def orderId = column[String]("order_id")

  def * = (id, name, amount, orderId) <>(PaymentGroup.tupled, PaymentGroup.unapply)
}

class ShippingGroupRepo(tag: Tag) extends Table[ShippingGroup](tag, "shipping_group") {
  def id = column[String]("id", O.PrimaryKey)

  def addressId = column[String]("address_id")

  def shippingMethod = column[String]("shipping_method")

  def orderId = column[String]("order_id")

  def * = (id, addressId, shippingMethod, orderId) <>(ShippingGroup.tupled, ShippingGroup.unapply)
}


object OrderState {
  val INITIAL: Int = 0
  val SUBMITTED: Int = 1
  val COMPLETE: Int = 2
  val CANCELLED: Int = 3
}

object Order extends ((String, String, Int, Option[String], Timestamp, Timestamp) => Order) {
  val log = Logger(this.getClass)

  def findOrderById(orderId: String)(implicit session: Session) = {
    TableQuery[OrderRepo].filter(_.id === orderId).firstOption
  }

  def fetchCommerceItems(order: Order)(implicit session: Session) = {
    val commerceItemQuery = TableQuery[CommerceItemRepo]
    val cis: List[CommerceItem] = commerceItemQuery.filter(_.orderId === order.id).list()
    if (log.isDebugEnabled)
      log.debug(s"commerceItems is $cis")
    cis
  }

  def calculateCommerceItemTotaPrice(ci: CommerceItem)(implicit session: Session) = {
    val priceInfo = TableQuery[PriceInfoRepo].where(_.id === ci.priceId).first()
    val totalActualPrice = priceInfo.actualPrice * ci.quantity
    val totalListPrice = priceInfo.listPrice * ci.quantity
    (totalListPrice, totalActualPrice)
  }

  /**
   * Price Order and persist order price information
   * @param order
   */
  def priceOrder(order: Order)(implicit session: Session) = {
    val commerceItems = fetchCommerceItems(order)
    val actualPrice = (commerceItems.map(calculateCommerceItemTotaPrice(_)._2) :\ (BigDecimal(0)))(_ + _)
    val listPrice = (commerceItems.map(calculateCommerceItemTotaPrice(_)._1) :\ (BigDecimal(0)))(_ + _)
    log.debug(s"totalActualPrice: $actualPrice, totalListPrice: $listPrice")
    val priceInfoRepo = TableQuery[PriceInfoRepo]
    order.priceId match {
      case Some(pid) => {
        priceInfoRepo.where(_.id === pid).update(PriceInfo(pid, listPrice, actualPrice, None))
      }
      case _ => {
        val priceInfoId = IdGenerator.generatePriceInfoId()
        priceInfoRepo.insert(PriceInfo(priceInfoId, listPrice, actualPrice, None))
        TableQuery[OrderRepo].where(_.id === order.id).update(Order(order.id, order.profileId, order.state, Some(priceInfoId), order.createdTime, new Timestamp(new Date().getTime)))
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
  def addItem(profileId: String, orderId: Option[String], skuId: String, quantity: Int) =
    DBHelper.database.withSession {
      implicit session =>
        val orderRepo = TableQuery[OrderRepo]
        val priceInfoRepo = TableQuery[PriceInfoRepo]
        val ciRepo = TableQuery[CommerceItemRepo]
        session.withTransaction {
          if (log.isDebugEnabled)
            log.debug(s"add new item with skuId $skuId, quantity $quantity")
          val order = orderId match {
            case Some(id) =>
              log.debug(s"Found existed order with id $orderId")
              orderRepo.where(_.id === orderId).first()
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
              val commerceItem = CommerceItem(ci.id, ci.skuId, order.id, ci.priceInfo.id, quantity, new Timestamp(new Date().getTime))
              ciRepo.where(_.id === ci.id).update(commerceItem)
            case _ =>
              //Add new commerceItem
              log.debug(s"No existed item found, add new!")
              val priceInfo = PriceInfo(IdGenerator.generatePriceInfoId(), sku.listPrice, sku.price, None)
              val commerceItem = CommerceItem(IdGenerator.generateCommerceItemId(), skuId, order.id, priceInfo.id, quantity, new Timestamp(new Date().getTime))
              priceInfoRepo.insert(priceInfo)
              ciRepo.insert(commerceItem)
          }
          Order.priceOrder(order)
          order
        }
    }


  def create(profileId: String) = {
    val order = Order(IdGenerator.generateOrderId(), profileId, OrderState.INITIAL, None, new Timestamp(new Date().getTime()), new Timestamp(new Date().getTime()))
    DBHelper.database.withTransaction {
      implicit ss: Session =>
        TableQuery[OrderRepo].insert(order)
        val profile = Profile.findUserById(profileId).get
        profile.defaultAddress match {
          case Some(address) =>
            TableQuery[ShippingGroupRepo].insert(ShippingGroup(IdGenerator.generateShippingGroupId(), address.id, Messages("sg.default.method"), order.id))
          case _ =>
            if (log.isDebugEnabled)
              log.debug("No address found on user")
        }
    }
    log.debug(s"order is created with id $order.id")
    order
  }

  def findOrderShippingGroup(orderId: String)(implicit session: Session) = {
    TableQuery[ShippingGroupRepo].where(_.orderId === orderId).firstOption
  }

  def updateShippingGroup(address: Address, order: Order)(implicit session: Session) = {
    val sgRepo = TableQuery[ShippingGroupRepo]
    Order.findOrderShippingGroup(order.id) match {
      case Some(sg) =>
        if (sg.addressId != address.id) {
          val shippingGroup = ShippingGroup(sg.id, address.id, sg.shippingMethod, sg.orderId)
          sgRepo.update(shippingGroup)
        }
      case _ =>
        if(log.isDebugEnabled)
          log.debug("INSERT new ShippingGroup with id " + address.id)
        sgRepo.insert(ShippingGroup(IdGenerator.generateShippingGroupId(), address.id, Messages("sg.default.method"), order.id))
    }
  }

}
