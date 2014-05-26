package models

import java.sql.Timestamp

import play.api.db.DB
import play.api.Play.current

import scala.slick.driver.MySQLDriver.simple._
import util.{DBHelper}
import java.util.Date
import play.api.Logger
import play.api.i18n.Messages
import scala.slick.lifted

/**
 * Created with IntelliJ IDEA.
 * User: simonwang
 * Date: 10/30/13
 * Time: 11:08 PM
 * To change this template use File | Settings | File Templates.
 */
case class Order(id: String, profileId: String, state: Int, priceId: Option[String], processFlag: Int, createdTime: Timestamp, modifiedTime: Timestamp) {
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
  lazy val paymentGroup = DBHelper.database.withSession {
    implicit session =>
      Order.findPaymentGroup(id)
  }
  lazy val orderStateDesc = OrderState.getOrderStateDesc(state)

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

case class CommerceItem(id: String, skuId: String, orderId: String, priceId: String, dinnerType: Int,  quantity: Int, createdTime: Timestamp) {
  lazy val sku = DBHelper.database.withSession {
    implicit session =>
      Product.findSkuById(skuId).get
  }
  lazy val priceInfo = DBHelper.database.withSession {
    implicit session =>
      TableQuery[PriceInfoRepo].where(_.id === priceId).first()
  }

  def totalActualPrice = priceInfo.actualPrice * quantity

  def totalListPrice = priceInfo.listPrice * quantity
}

case class PriceInfo(id: String, listPrice: BigDecimal, actualPrice: BigDecimal, promoDescription: Option[String])

case class PaymentGroup(id: String, paymentType: Int, amount: BigDecimal, orderId: String) {
  def paymentTypeDesc = PaymentType.getPaymentTypeDesc(paymentType)
}

case class ShippingGroup(id: String, addressId: String, shippingMethod: String, orderId: String)

class OrderRepo(tag: Tag) extends Table[Order](tag, "order") {
  def id = column[String]("id", O.PrimaryKey)

  def profileId = column[String]("profile_id")

  def state = column[Int]("state")

  def priceId = column[Option[String]]("price_id")

  def createdTime = column[Timestamp]("created_time")

  def modifiedTime = column[Timestamp]("modified_time")

  def processFlag = column[Int]("process_flag")

  def * = (id, profileId, state, priceId, processFlag, createdTime, modifiedTime) <>(Order.tupled, Order.unapply)

}

class CommerceItemRepo(tag: Tag) extends Table[CommerceItem](tag, "commerce_item") {
  def id = column[String]("id", O.PrimaryKey)

  def skuId = column[String]("sku_id")

  def orderId = column[String]("order_id")

  def priceId = column[String]("price_id")

  def dinnerType = column[Int]("dinner_type")

  def quantity = column[Int]("quantity")

  def createdTime = column[Timestamp]("created_time")

  def * = (id, skuId, orderId, priceId, dinnerType, quantity, createdTime) <>(CommerceItem.tupled, CommerceItem.unapply)

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

  def paymentType = column[Int]("payment_type")

  def amount = column[BigDecimal]("amount")

  def orderId = column[String]("order_id")

  def * = (id, paymentType, amount, orderId) <>(PaymentGroup.tupled, PaymentGroup.unapply)
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
  private val orderStateMap = Map(INITIAL -> "初始化", SUBMITTED -> "已提交", COMPLETE -> "完成", CANCELLED -> "已取消")

  def getOrderStateDesc(key: Int) = orderStateMap.get(key).get
}

object DinnerType{
  val BREAKFAST: Int = 0
  val LUNCH: Int = 1
  val SUPPER: Int = 2

  val dinnerTypeMap = Map(BREAKFAST -> "早餐", LUNCH -> "午餐", SUPPER -> "晚餐")
}

/**
 * Order Process Flag enumeration, it is a property of Order entity
 */
object OrderProcessFlag {
  //Initialization Flag
  val INITIAL_FLAG = 0
  //Stands for product is counted
  val PRODUCT_COUNT_FLAG = 1
  //Profile Points is calculated
  val PROFILE_POINT_FLAG = 2
  //Both Product Count and Profile Points is calculated
  val DONE_FLAG = 3
}

object PaymentType {
  val REACH_DEBIT: Int = 0
  val STORE_CREDIT: Int = 1
  private val paymentMap = Map(REACH_DEBIT -> "货到付款", STORE_CREDIT -> "积分支付")

  def getPaymentTypeDesc(key: Int) = paymentMap.get(key).get
}

object Order extends ((String, String, Int, Option[String], Int, Timestamp, Timestamp) => Order) {
  val log = Logger(this.getClass)

  val orderRepo = TableQuery[OrderRepo]

  def findOrderById(orderId: String)(implicit session: Session) = {
    orderRepo.filter(_.id === orderId).firstOption
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
        val priceInfoId = LocalIdGenerator.generatePriceInfoId()
        priceInfoRepo.insert(PriceInfo(priceInfoId, listPrice, actualPrice, None))
        orderRepo.where(_.id === order.id).update(Order(order.id, order.profileId, order.state, Some(priceInfoId), OrderProcessFlag.INITIAL_FLAG, order.createdTime, new Timestamp(new Date().getTime)))
      }
    }
    log.debug("price order done!")
  }


  /**
   * Add SKU to order, if order already contain this SKU, merge it, otherwise add new commerceItem
   * @param profileId
   * @param orderId
   * @param sku
   * @param quantity
   * @return
   */
  def addItem(profileId: String, orderId: Option[String], sku: Sku, quantity: Int, dinnerType: Int)(implicit session: Session) = {
    val priceInfoRepo = TableQuery[PriceInfoRepo]
    val ciRepo = TableQuery[CommerceItemRepo]
    if (log.isDebugEnabled)
      log.debug(s"add new item with skuId $sku, quantity $quantity")
    val order = orderId match {
      case Some(id) =>
        log.debug(s"Found existed order with id $orderId")
        orderRepo.where(_.id === orderId).first()
      case None =>
        log.debug(s"No order found, create new!")
        create(profileId)
    }
    //GET SKU Price
    val existedCi = fetchCommerceItems(order).filter(_.skuId == sku.id).filter(_.dinnerType == dinnerType).headOption
    existedCi match {
      case Some(ci) =>
        //Merge existed commerceItem
        log.debug(s"found existed commerce item")
        ciRepo.where(_.id === ci.id).update(ci.copy(quantity = ci.quantity + quantity))
      case _ =>
        //Add new commerceItem
        log.debug(s"No existed item found, add new!")
        val priceInfo = PriceInfo(LocalIdGenerator.generatePriceInfoId(), sku.listPrice, sku.price, None)
        val commerceItem = CommerceItem(LocalIdGenerator.generateCommerceItemId(), sku.id, order.id, priceInfo.id, dinnerType, quantity, new Timestamp(new Date().getTime))
        priceInfoRepo.insert(priceInfo)
        ciRepo.insert(commerceItem)
    }
    Order.priceOrder(order)
    order
  }

  def updateCommerceItemQuantity(profileId: String, orderId: String, skuId: String, quantity: Int)(implicit session: Session) = {
    val ciRepo = TableQuery[CommerceItemRepo]
    val order = findOrderById(orderId).get
    val existedCi = order.commerceItems.filter(_.skuId == skuId).head
    log.debug(s"found existed commerce item")
    ciRepo.where(_.id === existedCi.id).update(existedCi.copy(quantity = quantity))
    Order.priceOrder(order)
    order
  }


  def create(profileId: String) = {
    val order = Order(LocalIdGenerator.generateOrderId(), profileId, OrderState.INITIAL, None, OrderProcessFlag.INITIAL_FLAG, new Timestamp(new Date().getTime()), new Timestamp(new Date().getTime()))
    DBHelper.database.withTransaction {
      implicit ss: Session =>
        orderRepo.insert(order)
        val profile = Profile.findUserById(profileId).get
        profile.defaultAddress match {
          case Some(address) =>
            TableQuery[ShippingGroupRepo].insert(ShippingGroup(LocalIdGenerator.generateShippingGroupId(), address.id, Messages("sg.default.method"), order.id))
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
          sgRepo.where(_.id === sg.id).update(shippingGroup)
        }
      case _ =>
        if (log.isDebugEnabled)
          log.debug("INSERT new ShippingGroup with id " + address.id)
        sgRepo.insert(ShippingGroup(LocalIdGenerator.generateShippingGroupId(), address.id, Messages("sg.default.method"), order.id))
    }
  }

  def findPaymentGroup(orderId: String)(implicit session: Session) = {
    TableQuery[PaymentGroupRepo].filter(_.orderId === orderId).firstOption
  }

  def submitOrder(orderId: String)(implicit session: Session) = {
    val order = findOrderById(orderId).get
    priceOrder(order)
    val priceInfo = order.priceInfo
    val pgRepo = TableQuery[PaymentGroupRepo]
    findPaymentGroup(orderId) match {
      case Some(pg) =>
        pgRepo.where(_.id === pg.id).update(PaymentGroup(pg.id, pg.paymentType, priceInfo.actualPrice, pg.orderId))
      case _ =>
        pgRepo.insert(PaymentGroup(LocalIdGenerator.generatePaymentGroupId(), PaymentType.REACH_DEBIT, priceInfo.actualPrice, orderId))
    }
    val newOrder = order.copy(state = OrderState.SUBMITTED, modifiedTime = new Timestamp(new Date().getTime))
    if (log.isDebugEnabled)
      log.debug(s"new order is $newOrder")
    orderRepo.where(_.id === orderId).update(newOrder)
  }

  def countProduct(order: Order)(implicit session: Session) = {
    val items = fetchCommerceItems(order)
    val productVolumeRepo = TableQuery[ProductSalesVolumeRepo]
    for (item <- items) {
      val sku = Product.findSkuById(item.skuId).get
      val productVolume = ProductSalesVolume(sku.productId, item.quantity, new Timestamp(new Date().getTime))
      if (log.isDebugEnabled)
        log.debug("current item:" + item.id + " productId:" + sku.productId + " quantity:" + item.quantity)
      productVolumeRepo.where(_.productId === productVolume.productId).firstOption match {
        case Some(existedPv) =>
          productVolumeRepo.where(_.productId === productVolume.productId).update(productVolume.copy(volume = existedPv.volume + productVolume.volume))
        case _ =>
          productVolumeRepo.insert(productVolume)
      }
    }
  }

  def fetchOrderToBeProcessed(state: Int)(implicit session: Session) = {
    orderRepo.where(_.state === state).where(_.processFlag < OrderProcessFlag.DONE_FLAG).list()
  }

  def updateOrder(order: Order)(implicit session: Session) = {
    orderRepo.where(_.id === order.id).update(order)
  }

}
