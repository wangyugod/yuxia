package vo

import play.api.libs.json.{Json, JsArray, JsObject}

/**
 * Created with IntelliJ IDEA.
 * User: simonwang
 * Date: 8/23/13
 * Time: 8:26 AM
 * To change this template use File | Settings | File Templates.
 */

case class SearchResult(numFound: Int, start: Int, products: Seq[ProductSearchResult])

case class ProductSearchResult(id: String, name: String, description: Option[String], longDescription: Option[String], categories: Seq[String], prices: Seq[BigDecimal])

//case class SkuSearchResult(id:String, name:String, description:String, listPrice:BigDecimal, salePrice:BigDecimal, saleStartDate: Date, saleEndDate: Date)


object ProductSearchResult {
  def apply(jsObject: JsObject): ProductSearchResult = {
    val id = jsObject \ ("id")
    val name = jsObject \ ("name")
    val description = jsObject \ ("descrption")
    val longDescription = jsObject \ ("longDescription")
    val catList = (jsObject \ ("cat.id")).as[Seq[String]]
    val priceList = (jsObject \ ("price")).as[Seq[BigDecimal]]
    ProductSearchResult(id.as[String], name.as[String], description.as[Option[String]], longDescription.as[Option[String]], catList, priceList)
  }
}

object SearchResult {
  def apply(jsObject: JsObject): SearchResult = {
    val numFound = jsObject \ ("numFound")
    val start = jsObject \ ("start")
    val products = jsObject \ ("docs")
    val productObjects = products.as[Seq[JsObject]]
    val productSearchResults =
      for (product <- productObjects)
      yield ProductSearchResult(product)


    SearchResult(numFound.as[Int], start.as[Int], productSearchResults)
  }
}