package vo

import play.api.libs.json.{Json, JsArray, JsObject}
import play.api.i18n.Messages

/**
 * Created with IntelliJ IDEA.
 * User: simonwang
 * Date: 8/23/13
 * Time: 8:26 AM
 * To change this template use File | Settings | File Templates.
 */

case class SearchResult(numFound: Int, start: Int, products: Seq[ProductSearchResult])

case class ProductSearchResult(id: String, name: String, description: Option[String], longDescription: Option[String], categories: Seq[String], prices: Seq[BigDecimal], imageUrl: String){
  lazy val priceRange: (BigDecimal, BigDecimal) = {
    prices match {
      case Nil => (0, 0)
      case List(p) => (p, p)
      case p => {
        val list = prices.sortWith((price1, price2) => price1 < price2)
        (list.head, list.reverse.head)
      }
    }
  }
}

//case class SkuSearchResult(id:String, name:String, description:String, listPrice:BigDecimal, salePrice:BigDecimal, saleStartDate: Date, saleEndDate: Date)


object ProductSearchResult {
  def apply(jsObject: JsObject): ProductSearchResult = {
    val id = jsObject \ ("id")
    val name = jsObject \ ("name")
    val description = jsObject \ ("descrption")
    val longDescription = jsObject \ ("longDescription")
    val imageUrl = (jsObject \ ("image_url")).as[String]
    val catList = (jsObject \ ("cat.id")).as[Seq[String]]
    val priceList = (jsObject \ ("price")).as[Seq[BigDecimal]]
    ProductSearchResult(id.as[String], name.as[String], description.as[Option[String]], longDescription.as[Option[String]], catList, priceList, imageUrl)
  }

  def displayPrice(priceRange:(BigDecimal, BigDecimal)) = {
    priceRange match {
      case (x, y) if x == y => Messages("srp.price.single", x)
      case (x, y) => Messages("srp.price.range", x, y)
    }
  }
}

object SearchResult {
  def apply(jsObject: JsObject): SearchResult = {
    val numFound = (jsObject \ ("numFound")).as[Int]
    val start = jsObject \ ("start")
    if (numFound == 0) {
      SearchResult(numFound, start.as[Int], Nil)
    } else {
      val products = jsObject \ ("docs")
      val productObjects = products.as[Seq[JsObject]]
      val productSearchResults =
        for (product <- productObjects)
        yield ProductSearchResult(product)
      SearchResult(numFound, start.as[Int], productSearchResults)
    }
  }
}