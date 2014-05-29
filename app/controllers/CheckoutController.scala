package controllers

import play.api.mvc._
import play.api._
import views.html
import models._
import util.DBHelper
import play.api.i18n.Messages
import scala.Some
import play.api.cache.Cache
import play.api.Play.current
import play.api.libs.concurrent.Akka
import akka.actor.Props
import actors.{GetInventory, UpdateInventory, InventoryProcessActor}
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global

/**
 * Created with IntelliJ IDEA.
 * User: simonwang
 * Date: 11/21/13
 * Time: 11:43 PM
 * To change this template use File | Settings | File Templates.
 */
object CheckoutController extends Controller with Users with Secured with CacheController {
  val log = Logger(this.getClass)
  val inventoryActor = Akka.system.actorOf(Props(new InventoryProcessActor()))

  def addItemToCart(skuId: String, quantity: Int, dinnerType: Int) = isAuthenticated {
    implicit request => {
      DBHelper.database.withTransaction {
        implicit session =>
          val sku = getSku(skuId)

          val order = Order.addItem(request.session.get(USER_ID).get, request.session.get(CURR_ORDER_ID), sku, quantity, dinnerType)
          if (log.isDebugEnabled)
            log.debug("current order is " + request.session.get(CURR_ORDER_ID))
          request.session.get(CURR_ORDER_ID) match {
            case Some(orderId) => Ok("Add to Cart Successfully")
            case None =>
              val newSession = request.session + (CURR_ORDER_ID -> order.id)
              if (log.isDebugEnabled)
                log.debug("new session is " + newSession)
              Ok("SUCCESS").withSession(newSession)
          }
      }
    }
  }

  def viewCart() = isAuthenticated {
    implicit request => {
      val order = request.session.get(CURR_ORDER_ID) match {
        case Some(ordId) =>
          DBHelper.database.withSession {
            implicit session =>
              Order.findOrderById(ordId)
          }
        case _ =>
          None
      }
      Ok(html.checkout.cart(order))
    }
  }

  def updateItemQuantity(skuId: String, quantity: Int) = isAuthenticated {
    implicit request => {
      val profileId = request.session.get(USER_ID).get
      val orderId = request.session.get(CURR_ORDER_ID)
      if (log.isDebugEnabled)
        log.debug(s"update item $skuId quantity to $quantity")
      DBHelper.database.withTransaction {
        implicit session =>
          Order.updateCommerceItemQuantity(profileId, orderId.get, skuId, quantity)
      }
      Redirect(routes.CheckoutController.viewCart())
    }
  }

  def removeItem(itemId: String) = isAuthenticated {
    implicit request => {
      val orderId = request.session.get(CURR_ORDER_ID).get
      DBHelper.database.withTransaction {
        implicit session =>
          val order = Order.findOrderById(orderId).get
          order.removeCommerceItem(itemId)
      }
      Redirect(routes.CheckoutController.viewCart())
    }
  }

  def checkout() = isAuthenticated {
    implicit request =>
      val orderId = request.session.get(CURR_ORDER_ID).get
      if (log.isDebugEnabled)
        log.debug(s"Found order in session with id $orderId")
      val order = DBHelper.database.withSession {
        implicit session =>
          Order.findOrderById(orderId).get
      }
      Ok(html.checkout.checkout(order))
  }


  def updateShippingGroup = isAuthenticated {
    implicit request =>
      ProfileController.addressForm.bindFromRequest.fold(
        formWithErrors => {
          if (log.isDebugEnabled)
            log.debug("here eror form now")
          Redirect(routes.CheckoutController.checkout()).flashing("addrError" -> Messages("checkout.address.error"))
        },
        address => {
          val userId = request.session.get(USER_ID).get
          if (log.isDebugEnabled)
            log.debug("is default address:" + address.isDefaultAddress)
          val orderId = request.session.get(CURR_ORDER_ID).get
          DBHelper.database.withTransaction {
            implicit session =>
              Address.saveOrUpdate(userId, address.address, address.isDefaultAddress.getOrElse(false))
              val order = Order.findOrderById(orderId).get
              Order.updateShippingGroup(address.address, order)
          }
          Redirect(routes.CheckoutController.checkout())
        }
      )
  }

  def removeShippingAddress(addressId: String) = isAuthenticated {
    implicit request =>
      val userId = request.session.get(USER_ID).get
      val orderId = request.session.get(CURR_ORDER_ID).get
      DBHelper.database.withTransaction {
        implicit session =>
          val order = Order.findOrderById(orderId).get
          val existedShippingGroup = Order.findOrderShippingGroup(orderId)
          Address.deleteUserAddress(userId, addressId)
          if (existedShippingGroup.isDefined && existedShippingGroup.get.addressId == addressId) {
            val defaultAddress = Profile.findDefaultAddress(userId) match {
              case Some(address) =>
                Order.updateShippingGroup(address, order)
              case _ =>
                log.debug("NO default order found, the profile doesn't have any address now")
            }
            log.debug(s"default shipping address is $defaultAddress")
          }
      }
      Redirect(routes.CheckoutController.checkout())
  }

  def submitOrder = isAuthenticated {
    implicit request =>
      val orderId = request.session.get(CURR_ORDER_ID).get
      DBHelper.database.withTransaction {
        implicit session =>
          //Check Shipping Group
          val sg = Order.findOrderShippingGroup(orderId)
          if (log.isDebugEnabled)
            log.debug(s"shippingGroup is $sg")
          if (sg.isEmpty) {
            Redirect(routes.CheckoutController.checkout()).flashing("result" -> "fail", "errorMsg" -> Messages("order.submit.error"))
          } else {
            //Check and Update Inventory
            var itemList = List.empty[(String, Int)]
            Order.fetchCommerceItems(orderId).foreach {
              ci =>
                val productId = Product.findSkuById(ci.skuId).get.productId
                itemList = (productId, ci.quantity) :: itemList
            }
            implicit val timeout = Timeout(5 seconds)
            val resultFuture = inventoryActor ? UpdateInventory(itemList)
            Async {
              resultFuture.mapTo[(List[String], String)].map {
                result => {
                  if (log.isDebugEnabled)
                    log.debug("update inventory result is " + result._1 + " " + result._2)
                  val badItemList = result._1
                  if (badItemList.nonEmpty) {
                    Redirect(routes.CheckoutController.checkout()).flashing("result" -> "fail", "errorMsg" -> Messages("order.inventory.error", badItemList.mkString(",")))
                  } else {
                    Order.submitOrder(orderId)
                    val newSession = request.session - CURR_ORDER_ID + (LAST_ORDER_ID -> orderId)
                    Redirect(routes.CheckoutController.thankYou).withSession(newSession).flashing("result" -> "success")
                  }
                }
              }
            }
          }
      }
  }


  def thankYou = isAuthenticated {
    implicit request =>
      val orderId = request.session.get(LAST_ORDER_ID).get
      val currOrderId = request.session.get(CURR_ORDER_ID)
      if (log.isDebugEnabled)
        log.debug(s"lastOrderId is $orderId currentId is $currOrderId")
      val order = DBHelper.database.withSession {
        implicit session =>
          Order.findOrderById(orderId).get
      }
      Ok(html.checkout.thankyou(order))
  }


}
