package controllers

import play.api.mvc._
import play.api._
import views.html
import models.{ShippingGroup, Address, Profile, Order}
import util.DBHelper

/**
 * Created with IntelliJ IDEA.
 * User: simonwang
 * Date: 11/21/13
 * Time: 11:43 PM
 * To change this template use File | Settings | File Templates.
 */
object CheckoutController extends Controller with Users with Secured {
  val log = Logger(this.getClass)

  def addItemToCart(skuId: String, quantity: Int) = isAuthenticated {
    implicit request => {
      val order = Order.addItem(request.session.get(USER_ID).get, request.session.get(CURR_ORDER_ID), skuId, quantity)
      if (log.isDebugEnabled)
        log.debug("current order is " + request.session.get(CURR_ORDER_ID))
      request.session.get(CURR_ORDER_ID) match {
        case Some(orderId) => Ok("Add to Cart Successfully")
        case None =>
          val newSession = request.session + (CURR_ORDER_ID -> order.id)
          if (log.isDebugEnabled)
            log.debug("new session is " + newSession)
          Ok("Successfully").withSession(newSession)
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
      Order.addItem(profileId, orderId, skuId, quantity)
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
          Redirect(routes.CheckoutController.checkout())
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


}
