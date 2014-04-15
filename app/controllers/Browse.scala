package controllers

import play.api.mvc._
import play.api._
import views.html
import models.Product
import util.{DBHelper, SearchHelper}
import play.api.libs.json.{JsString, JsObject, Json}
import vo.SearchResult

/**
 * Created with IntelliJ IDEA.
 * User: simonwang
 * Date: 8/15/13
 * Time: 2:31 PM
 * To change this template use File | Settings | File Templates.
 */
object Browse extends Controller with Users {
  private val log = Logger(this.getClass)

  def productDetail(id: String) = Action {
    implicit request => {
      val product = Product.findById(id)
      product match {
        case Some(p) => Ok(html.browse.pdp(p))
        case _ => Redirect(routes.Application.pageNotFound())
      }
    }
  }

  def searchByKeyword = Action {
    implicit request => {
      val keywords = request.queryString.get("q").get.head
      val searchResult = SearchResult(SearchHelper.query(SearchHelper.PRODUCT_SEARCH, "text", keywords, Map.empty[String, String], request))
      Ok(html.browse.srp("Search Result Page", searchResult))
    }
  }

  def categoryLanding(id: String) = Action {
    implicit request => {
      val fqMap = request.getQueryString("area") match {
        case Some(areaId) => Map("area.id" -> areaId)
        case _ => Map.empty[String, String]
      }
      val searchResult = SearchResult(SearchHelper.query(SearchHelper.PRODUCT_SEARCH, "cat.id", id, fqMap, request))
      Ok(html.browse.category("Search Result Page", searchResult))
    }
  }

  def selectSku(id: String) = Action {
    implicit request => {
      val sku = DBHelper.database.withSession {
        implicit session =>
          Product.findSkuById(id).get
      }
      Ok(JsObject(List("skuId" -> JsString(sku.id), "price" -> JsString(sku.price.toString()))))
    }
  }
}
