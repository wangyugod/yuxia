package controllers

import play.api.mvc._
import play.api._
import views.html
import models.Order

/**
 * Created with IntelliJ IDEA.
 * User: simonwang
 * Date: 11/21/13
 * Time: 11:43 PM
 * To change this template use File | Settings | File Templates.
 */
object CheckoutController extends Controller with Users with Secured{

  def addItemToCart(skuId: String, quantity: Int) = isAuthenticated{
    implicit request => {
      Order.addItem(request.session.get(USER_ID).get, request.session.get(CURR_ORDER_ID), skuId, quantity)
      Ok("Add To Cart with " + skuId + " qty:" + quantity)
    }
  }

}
