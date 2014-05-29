package controllers

import play.api.mvc._
import play.api._
import play.api.Play.current
import views.html
import models.{Sku, Product}
import util.{DBHelper, SearchHelper}
import play.api.libs.json.{JsString, JsObject, Json}
import vo.SearchResult
import play.api.cache.{Cached, Cache}

/**
 * Created with IntelliJ IDEA.
 * User: simonwang
 * Date: 8/15/13
 * Time: 2:31 PM
 * To change this template use File | Settings | File Templates.
 */
object Browse extends Controller with Users with CacheController {
  private val log = Logger(this.getClass)

  def productDetail(id: String) =
    Action {
      implicit request => {
        val product = DBHelper.database.withSession {
          implicit session =>
            getProduct(id)
        }
        Ok(html.browse.pdp(product))
      }
    }

  def searchByKeyword = Action {
    implicit request => {
      val keywords = request.queryString.get("q").get.head
      val searchResult = SearchResult(SearchHelper.query(SearchHelper.PRODUCT_SEARCH, "text", keywords, Map.empty[String, String], request))
      Ok(html.browse.srp("Search Result Page", searchResult, rankList))
    }
  }

  def rankList = {
    Cache.getAs[List[Product]]("ranklist") match {
      case Some(products) => products
      case _ =>
        DBHelper.database.withSession {
          implicit session =>
            val products = Product.fetchTopSellerProducts()
            Cache.set("ranklist", products, Play.current.configuration.getInt("cache.ttl").getOrElse(5))
            products
        }
    }

  }

  def categoryLanding(id: String) = Cached("category-" + id, Play.current.configuration.getInt("cache.ttl").getOrElse(5)) {
    Action {
      implicit request => {
        val fqMap = request.getQueryString("area") match {
          case Some(areaId) => Map("area.id" -> areaId)
          case _ => Map.empty[String, String]
        }
        val searchResult = SearchResult(SearchHelper.query(SearchHelper.PRODUCT_SEARCH, "cat.id", id, fqMap, request))
        Ok(html.browse.category("Search Result Page", searchResult, rankList))
      }
    }
  }

  def selectSku(id: String) = Action {
    implicit request => {
      val sku = DBHelper.database.withSession {
        implicit session =>
          getSku(id)
      }
      Ok(JsObject(List("skuId" -> JsString(sku.id), "price" -> JsString(sku.price.toString()), "listPrice" -> JsString(sku.listPrice.toString()))))
    }
  }

}
