package controllers

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import validation.Constraints
import views.html
import models._
import helper._

/**
 * Created with IntelliJ IDEA.
 * User: simonwang
 * Date: 7/1/13
 * Time: 8:00 PM
 * To change this template use File | Settings | File Templates.
 */
object Products extends Controller with Merchants {

  val productForm:Form[Product]= Form(
    mapping(
      "id" -> optional(text),
      "merchantId" -> text,
      "name" -> nonEmptyText,
      "description" -> nonEmptyText,
      "longDescription" -> nonEmptyText,
      "startDate" -> text,
      "endDate" -> text,
      "categories" -> text
    )((id, merchantId, name, description, longDescription, startDate, endDate, categories) => {
      val prod =  Product(id.getOrElse(IdGenerator.generateProfileId()), name, description, longDescription, AppHelper.convertDateFromText(Some(startDate)).get, AppHelper.convertDateFromText(Some(endDate)).get, merchantId)
      prod.categories = categories
      prod
    })
      ((x: Product) => Some(Some(x.id), x.merchantId, x.name, x.description, x.longDescription, AppHelper.convertDateToText(Some(x.startDate)).get, AppHelper.convertDateToText(Some(x.endDate)).get, x.categories))
  )

  def newProduct = Action {
    implicit request => {
      Ok(html.merchandise.product(productForm))
    }
  }

  def create = Action {
    implicit request => {
      productForm.bindFromRequest().fold(
        formWithErrors =>{
          println("form is " + formWithErrors)
          BadRequest(html.merchandise.product(formWithErrors))
        },
        form => {
          val catIds = form.categories.split(",")
          Product.create(form, catIds)
          productForm.fill(form)
          Redirect(routes.Products.newProduct())
        }
      )
    }
  }

  def list = Action {
    implicit request => {
      val merchantId = request.session.get(MERCHANT_ID).get
      Ok(html.merchandise.productlist(Product.findByMerchantId(merchantId)))
    }
  }


}
