package controllers

import play.api.mvc._
import play.api._
import views.html
import models.Product

/**
 * Created with IntelliJ IDEA.
 * User: simonwang
 * Date: 8/15/13
 * Time: 2:31 PM
 * To change this template use File | Settings | File Templates.
 */
object Browse extends Controller with Users {
  def productDetail(id: String) = Action {
    implicit request => {
      val product = Product.findById(id)
      product match {
        case Some(p) => Ok(html.browse.pdp(p))
        case _ => Redirect(routes.Application.pageNotFound())
      }
    }
  }

}
