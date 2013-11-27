package controllers

import play.api.mvc._
import play.api._
import views.html

/**
 * Created with IntelliJ IDEA.
 * User: simonwang
 * Date: 11/21/13
 * Time: 11:43 PM
 * To change this template use File | Settings | File Templates.
 */
object CheckoutController extends Controller{

  def addItemToCart(skuId: String, quantity: String) = Action{
    implicit request => {
      Ok("Add To Cart with " + skuId + " qty:" + quantity)
    }
  }

}
