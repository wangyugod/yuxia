package controllers

import play.api.mvc._
import play.api._
import views.html
import models.{Profile, Order}

/**
 * Created with IntelliJ IDEA.
 * User: simonwang
 * Date: 11/21/13
 * Time: 11:43 PM
 * To change this template use File | Settings | File Templates.
 */
object CheckoutController extends Controller with Users with Secured{
  val log = Logger(this.getClass)
  def addItemToCart(skuId: String, quantity: Int) = isAuthenticated{
    implicit request => {
      val order = Order.addItem(request.session.get(USER_ID).get, request.session.get(CURR_ORDER_ID), skuId, quantity)
      if(log.isDebugEnabled)
        log.debug("current order is " + request.session.get(CURR_ORDER_ID))
      request.session.get(CURR_ORDER_ID) match {
        case Some(orderId) => Ok("Add to Cart Successfully")
        case None =>
          val newSession = request.session + (CURR_ORDER_ID -> order.id)
          if(log.isDebugEnabled)
            log.debug("new session is " + newSession)
          Ok("Successfully").withSession(newSession)
      }
    }
  }

  def viewCart() = isAuthenticated{
    implicit request => {
      val order = request.session.get(CURR_ORDER_ID) match {
        case Some(ordId) =>
          Order.findOrderById(ordId)
        case _ =>
          None
      }
      Ok(html.checkout.cart(order))
    }
  }

  def updateItemQuantity(skuId: String, quantity: Int) = isAuthenticated{
    implicit request => {
      val profileId = request.session.get(USER_ID).get
      val orderId = request.session.get(CURR_ORDER_ID)
      if(log.isDebugEnabled)
        log.debug(s"update item $skuId quantity to $quantity")
      Order.addItem(profileId, orderId,skuId, quantity)
      Redirect(routes.CheckoutController.viewCart())
    }
  }

  def removeItem(itemId: String) = isAuthenticated{
    implicit request => {
      val orderId = request.session.get(CURR_ORDER_ID).get
      val order = Order.findOrderById(orderId).get
      order.removeCommerceItem(itemId)
      Redirect(routes.CheckoutController.viewCart())
    }
  }

  def checkout() = isAuthenticated{
    implicit request =>
      val orderId = request.session.get(CURR_ORDER_ID).get
      if(log.isDebugEnabled)
        log.debug(s"Found order in session with id $orderId")
      val order = Order.findOrderById(orderId).get
      Ok(html.checkout.checkout(order))
  }

}
