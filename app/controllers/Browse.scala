package controllers

import play.api.mvc._
import play.api._
import views.html
import models.Product
import helper.SearchHelper
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
  def productDetail(id: String) = Action {
    implicit request => {
      val product = Product.findById(id)
      product match {
        case Some(p) => Ok(html.browse.pdp(p))
        case _ => Redirect(routes.Application.pageNotFound())
      }
    }
  }

  def searchResult = Action{
    val params = Map("q" -> "name:炒饭", "wt" -> "json", "indent" -> "true")
    val result = SearchHelper.query(params)
    val obj = (result \ ("response")).as[JsObject]
    val searchResult = SearchResult(obj)
    println("search result is " + searchResult)
    Ok(Json.toJson(result \ ("response")))
  }

}
