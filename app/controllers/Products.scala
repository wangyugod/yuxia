package controllers

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import validation.Constraints
import views.html
import models._
import helper._
import java.io.File
import play.api.libs.json.{JsString, JsArray, JsObject}

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
      "categories" -> text,
      "selectedCat" -> optional(text)
    )
    {
      case (id, merchantId, name, description, longDescription, startDate, endDate, categories, selectedCat) =>{
        val productId = IdGenerator.generateProductId()
        (Product(id.getOrElse(productId), name, description, longDescription, AppHelper.convertDateFromText(Some(startDate)).get, AppHelper.convertDateFromText(Some(endDate)).get, merchantId, productId + ".jpg"), categories)
        }
      }
    {
      case(x: (Product, String)) =>{
        val catIds = x._2.split(",")
        val categories = Category.findByIds(catIds)
        var s = "";
        for(cat <- categories){
          s = s + "," + cat.name
        }
        s = s.substring(1)
        Some(Some(x._1.id), x._1.merchantId, x._1.name, x._1.description, x._1.longDescription, AppHelper.convertDateToText(Some(x._1.startDate)).get, AppHelper.convertDateToText(Some(x._1.endDate)).get, x._2, Some(s))
      }
    }

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
          println("id is " + form._1.id)
          println("form is " + form)
          request.body.file("image").map {
            file => file.ref.moveTo(new File(AppHelper.productImageDir + form._1.id + ".jpg"))
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

  def update = Action(parse.multipartFormData) {
    implicit request =>
      productForm.bindFromRequest().fold(
        formWithErrors => BadRequest(html.merchandise.product(formWithErrors)),
        form => {
          request.body.file("image").map {
            file => {
              val dir: String = AppHelper.productImageDir
              println("image file path " + dir + form._1.id)
              file.ref.moveTo(new File(dir + form._1.id + ".jpg"))
            }
          }
          val catIds = form._2.split(",")
          Product.update(form._1, catIds)
          productForm.fill(form)
          Redirect(routes.Products.get(form._1.id))
        }
      )
  }

  def delete(id: String) = Action {
    implicit request => {
      Product.delete(id)
      Redirect(routes.Products.list())
    }
  }

  def initCategoryTree = Action {
    implicit request => {
      val rootCategories = Category.rootCategories()
      Ok(JsArray(catToJson(rootCategories)))
    }
  }

  def childCategory(id:String) = Action {
    implicit request => {
      val childCategory = Category.childCategories(id);
      Ok(JsArray(catToJson(childCategory)))
    }
  }

  def rootCatJson(cat:Category) = {
    if(cat.isRoot){
      "isFolder" -> JsString("true")
    } else {
      "isFolder" -> JsString("false")
    }
  }

  def catToJson(categories: Seq[Category]) = {
    categories.map(
      cat =>
        JsObject(List("title" -> JsString(cat.name), "key" -> JsString(cat.id), "isLazy" -> JsString("true"), rootCatJson(cat)))
    )
  }


}
