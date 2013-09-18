package vo

import play.api.libs.json.JsObject

/**
 * Created with IntelliJ IDEA.
 * User: simonwang
 * Date: 9/18/13
 * Time: 6:08 PM
 * To change this template use File | Settings | File Templates.
 */
case class AreaSearchResult(numFound: Int, areas: Seq[AreaEntity])
case class AreaEntity(id: String, name: String, detail: String)

object AreaSearchResult {
  def apply(jsObject: JsObject): AreaSearchResult = {
    val responseBody = jsObject \ "response"
    val numFound = (responseBody \ ("numFound")).as[Int]
    if (numFound == 0) {
      AreaSearchResult(numFound, Nil)
    } else {
      val areas = responseBody \ ("docs")
      val areaObjects = areas.as[Seq[JsObject]]
      val areaSearchResults =
        for (area <- areaObjects)
        yield AreaEntity(area)
      AreaSearchResult(numFound, areaSearchResults)
    }
  }
}

object AreaEntity {
  def apply(jsObject: JsObject): AreaEntity = {
    val id = jsObject \ ("id")
    val name = jsObject \ ("name")
    val detail = jsObject \ ("detail")
    AreaEntity(id.as[String], name.as[String], detail.as[String])
  }
}