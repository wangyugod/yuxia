package controllers

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import views.html
import models._
import util._
import java.io.File
import play.api.libs.json.{JsString, JsArray, JsObject}
import vo._
import play.api.Logger

/**
 * Created with IntelliJ IDEA.
 * User: simonwang
 * Date: 7/1/13
 * Time: 8:00 PM
 * To change this template use File | Settings | File Templates.
 */
object Products extends Controller with Merchants with MerchSecured with CacheController {
  private val log = Logger(this.getClass)

  val productForm: Form[ProductVo] = Form(
    mapping(
      "id" -> optional(text),
      "merchantId" -> text,
      "name" -> nonEmptyText,
      "description" -> nonEmptyText,
      "longDescription" -> nonEmptyText,
      "startDate" -> optional(text),
      "endDate" -> optional(text),
      "categories" -> text,
      "selectedCat" -> nonEmptyText,
      "updateDaily" -> optional(boolean),
      "inventory" -> optional(number),
      "childSkus" -> seq(mapping(
        "skuId" -> optional(text),
        "skuName" -> text,
        "skuDesc" -> optional(text),
        "listPrice" -> bigDecimal,
        "salePrice" -> optional(bigDecimal),
        "saleStartDate" -> optional(text),
        "saleEndDate" -> optional(text)
      )(SkuVo.apply)(SkuVo.unapply)
      )
    )(ProductVo.apply)(ProductVo.unapply _)

  )

  def newProduct = isAuthenticated(implicit request => Ok(html.merchandise.newproduct(productForm)))

  def create = isAuthenticatedWithMultipartParser(parse.multipartFormData) {
    implicit request => {
      productForm.bindFromRequest().fold(
        formWithErrors => BadRequest(html.merchandise.newproduct(formWithErrors)),
        form => {
          println("form is " + form)
          request.body.file("image").map {
            file => file.ref.moveTo(new File(AppHelper.productImageDir + form.productId + ".jpg"), true)
          }
          val catIds = form.categories.split(",")
          Product.create(form.product, catIds, form.childSkus, form.dailyUpdate, form.inventory)
          productForm.fill(form)
          Redirect(routes.Products.newProduct())
        }
      )
    }
  }

  def list = isAuthenticated {
    implicit request => {
      val merchantId = request.session.get(MERCHANT_ID).get
      Ok(html.merchandise.productlist(Product.findByMerchantId(merchantId)))
    }
  }


  def get(id: String) = Action {
    implicit request => {
      val product = DBHelper.database.withSession {
        implicit session =>
          getProduct(id)
      }
      Ok(html.merchandise.product(productForm.fill(ProductVo(product))))
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
              file.ref.moveTo(new File(dir + form.productId + ".jpg"), true)
            }
          }
          if (log.isDebugEnabled)
            log.debug("form update daily is " + form.dailyUpdate)
          Product.update(form.product, form.categories.split(","), form.childSkus, form.dailyUpdate, form.inventory)
          productForm.fill(form)
          Redirect(routes.Products.get(form.productId))
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
      val rootCategories = DBHelper.database.withSession {
        implicit session =>
          Category.rootCategories()
      }
      Ok(JsArray(catToJson(rootCategories)))
    }
  }

  def childCategory(id: String) = Action {
    implicit request => {
      val childCategory = Category.childCategories(id);
      Ok(JsArray(catToJson(childCategory)))
    }
  }

  def rootCatJson(cat: Category) = {
    if (cat.isRoot) {
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
