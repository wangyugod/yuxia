package vo

import play.api.libs.json.{Json, JsArray, JsObject}
import play.api.i18n.Messages
import play.api.Logger

/**
 * Created with IntelliJ IDEA.
 * User: simonwang
 * Date: 8/23/13
 * Time: 8:26 AM
 * To change this template use File | Settings | File Templates.
 */

case class SearchResult(numFound: Int, start: Int, pageQty: Int, currentPage: Int, keyword: String, areaId: Option[String], products: Seq[ProductSearchResult])

case class ProductSearchResult(id: String, name: String, description: Option[String], longDescription: Option[String], categories: Seq[String], prices: Seq[BigDecimal], listPrices: Seq[BigDecimal], imageUrl: String, salesVolume: Int) {
  lazy val priceRange: (BigDecimal, BigDecimal) = ProductSearchResult.priceRange(prices)
  lazy val listPriceRange = ProductSearchResult.priceRange(listPrices)

//case class SkuSearchResult(id:String, name:String, description:String, listPrice:BigDecimal, salePrice:BigDecimal, saleStartDate: Date, saleEndDate: Date)

}
object ProductSearchResult {
  def apply(jsObject: JsObject): ProductSearchResult = {
    val id = jsObject \ ("id")
    val name = jsObject \ ("name")
    val description = jsObject \ ("descrption")
    val longDescription = jsObject \ ("longDescription")
    val imageUrl = (jsObject \ ("image_url")).as[String]
    val volume = (jsObject \ ("volume")).as[Option[Int]]
    val catList = (jsObject \ ("cat.id")).as[Seq[String]]
    val priceList = (jsObject \ ("price")).as[Seq[BigDecimal]]
    val listPriceList = (jsObject \ ("sku.listprice")).as[Seq[BigDecimal]]
    ProductSearchResult(id.as[String], name.as[String], description.as[Option[String]], longDescription.as[Option[String]], catList, priceList, listPriceList, imageUrl, volume.getOrElse(0))
  }

  def priceRange(prices: Seq[BigDecimal]):(BigDecimal, BigDecimal) = prices match {
    case Nil => (0, 0)
    case List(p) => (p, p)
    case p => {
      val list = prices.sortWith((price1, price2) => price1 < price2)
      (list.head, list.reverse.head)
    }
  }

  def displayPrice(priceRange: (BigDecimal, BigDecimal)) = {
    priceRange match {
      case (x, y) if x == y => Messages("srp.price.single", x)
      case (x, y) => Messages("srp.price.range", x, y)
    }
  }
}

object SearchResult {
  private val log = Logger(this.getClass)

  def apply(jsObject: JsObject): SearchResult = {

    val responseBody = jsObject \ "response"
    val requestParams = jsObject \ "responseHeader" \ "params"
    val numFound = (responseBody \ ("numFound")).as[Int]
    val start = (responseBody \ ("start")).as[Int]
    val area = (requestParams \ "fq").asOpt[String] match{
      case Some(areaFq) if(areaFq.startsWith("area.id:")) => Some(areaFq.substring(8))
      case _ => None
    }

    if (log.isDebugEnabled)
      log.debug("parameters is " + requestParams.toString())
    val rows = (requestParams \ "rows").as[String].toInt
    val currentPage = start / rows + 1
    val pageQty: Int = numFound % rows match {
      case 0 => numFound / rows
      case _ => numFound / rows + 1
    }
    val keyword = (requestParams \ "q").as[String].split(":")(1)
    if (numFound == 0) {
      SearchResult(numFound, start, 1, 1, keyword, area, Nil)
    } else {
      val products = responseBody \ ("docs")
      val productObjects = products.as[Seq[JsObject]]
      val productSearchResults =
        for (product <- productObjects)
        yield ProductSearchResult(product)
      SearchResult(numFound, start, pageQty, currentPage, keyword, area, productSearchResults)
    }
  }
}