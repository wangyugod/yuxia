package controllers

import play.api.mvc._
import play.api._
import views.html
import models.Product
import util.SearchHelper
import play.api.libs.json.{JsObject, Json}
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

  def search = Action {
    implicit request => {
      val keywords = request.queryString.get("q").get.head
      val pageParameter = request.queryString.get("page")
      val pageNum: Int = pageParameter match {
        case Some(x) => x.head.toInt - 1
        case _ => 0
      }
      if (log.isDebugEnabled)
        log.debug("current page is " + pageNum)
      val rows = Play.current.configuration.getInt("pagination.quantity").get
      val start = pageNum * rows
      val params = Map("q" -> ("text:" + keywords), "wt" -> "json", "indent" -> "true", "rows" -> rows.toString, "start" -> start.toString)
      val result = SearchHelper.query(params)
      val searchResult = SearchResult(result)
      if (log.isDebugEnabled)
        log.debug("search result is " + searchResult)
      Ok(html.browse.srp("Search Result Page", searchResult))
    }
  }
}
