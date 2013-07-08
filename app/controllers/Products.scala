package controllers

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import validation.Constraints
import views.html
import models._
import helper._
import java.io.File

/**
 * Created with IntelliJ IDEA.
 * User: simonwang
 * Date: 7/1/13
 * Time: 8:00 PM
 * To change this template use File | Settings | File Templates.
 */
object Products extends Controller with Merchants {

  val productForm: Form[(Product, String)] = Form(
    mapping(
      "id" -> optional(text),
      "merchantId" -> text,
      "name" -> nonEmptyText,
      "description" -> nonEmptyText,
      "longDescription" -> nonEmptyText,
      "startDate" -> text,
      "endDate" -> text,
      "categories" -> text
    ){ val productId = IdGenerator.generateProductId()
      ((id, merchantId, name, description, longDescription, startDate, endDate, categories) => (Product(id.getOrElse(productId), name, description, longDescription, AppHelper.convertDateFromText(Some(startDate)).get, AppHelper.convertDateFromText(Some(endDate)).get, merchantId, productId + ".jpg"), categories))}
      ((x: (Product, String)) => Some(Some(x._1.id), x._1.merchantId, x._1.name, x._1.description, x._1.longDescription, AppHelper.convertDateToText(Some(x._1.startDate)).get, AppHelper.convertDateToText(Some(x._1.endDate)).get, x._2))
  )

  def newProduct = Action {
    implicit request => {
      Ok(html.merchandise.newproduct(productForm))
    }
  }

  def create = Action(parse.multipartFormData) {
    implicit request => {
      productForm.bindFromRequest().fold(
        formWithErrors => BadRequest(html.merchandise.newproduct(formWithErrors)),
        form => {
          request.body.file("image").map{
            file => file.ref.moveTo(new File("/tmp/"+form._1.id + ".jpg"))
          }
          val catIds = form._2.split(",")
          Product.create(form._1, catIds)
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

  def get(id: String) = Action {
    implicit request => {
      val product = Product.findById(id)
      product match {
        case Some(p) => {
          val categories = p.categories
          var s = ("" /: categories)(_ + "," + _)
          s = s.substring(1)
          Ok(html.merchandise.product(productForm.fill((p, s))))
        }
        case None => {
          BadRequest(html.pageNotFound())
        }
      }
    }
  }

  def update = Action {
    implicit request => {
      Ok("updated")
    }
  }

  def delete(id:String) = Action{
    implicit request => {
      val result = Product.delete(id)
      Redirect(routes.Products.list())
    }
  }

}
