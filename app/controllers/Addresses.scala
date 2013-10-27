package controllers

import _root_.util.{SearchHelper, IdGenerator}
import play.api.mvc._
import play.api.Logger
import models.{Address, Area}
import play.api.libs.json._
import play.api.data._
import play.api.data.Forms._
import vo.{AreaSearchResult, SearchResult}

/**
 * Created with IntelliJ IDEA.
 * User: simonwang
 * Date: 9/11/13
 * Time: 11:28 PM
 * To change this template use File | Settings | File Templates.
 */
object Addresses extends Controller {
  private val log = Logger(this.getClass())

  def initAreaTrees = Action {
    implicit request => {
      val rootAreas = Area.rootAreas()
      if (log.isDebugEnabled)
        log.debug("ROOT AREAs: " + rootAreas)
      Ok(JsArray(areaJson(rootAreas)))
    }
  }


  def childAreas(id: String) = Action {
    implicit request => {
      val childAreas = Area.childAreas(id)
      if (log.isDebugEnabled)
        log.debug("CHILD Areas: " + childAreas)
      Ok(JsArray(areaJson(childAreas)))
    }
  }

  def searchArea(keyword: String) = Action{
    implicit request => {
      val searchResult = AreaSearchResult(SearchHelper.query(SearchHelper.ADDRESS_SEARCH, "text", keyword, Map.empty[String, String], request))
      val results = searchResult.areas.map(area =>
          JsObject(List("label" -> JsString(area.name), "value" -> JsString(area.id), "detail" -> JsString(area.detail)))
      )
      Ok(JsArray(results))
    }
  }


  private def areaJson(areaList: Seq[Area]) = {
    areaList.map(area => JsObject(List("title" -> JsString(area.name), "key" -> JsString(area.id), "tooltip" -> JsString(area.detail), "isLazy" -> JsString("true"), (if (area.isRoot) "isFolder" -> JsString("true") else "isFolder" -> JsString("false")))))
  }

}
