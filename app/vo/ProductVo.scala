package vo

import java.sql.{Timestamp, Date}
import models._
import util._
import play.api.Logger

/**
 * Created with IntelliJ IDEA.
 * User: Administrator
 * Date: 13-8-4
 * Time: 下午3:59
 * To change this template use File | Settings | File Templates.
 */
case class ProductVo(id: Option[String], merchantId: String, name: String, description: String, longDescription: String, startDate: Option[String], endDate: Option[String], categories: String, selectedCat: String, dailyUpdate: Option[Boolean], inventory: Option[Int], skus: Seq[SkuVo]) {
  val productId = id.getOrElse(LocalIdGenerator.generateProductId())
  var imageUrl = ""

  def product = {
    Product(productId, name, description, longDescription, AppHelper.convertDateFromText(startDate), AppHelper.convertDateFromText(endDate), merchantId, productId + ".jpg", new Timestamp(new java.util.Date().getTime()))
  }

  def childSkus: Seq[Sku] = {
    skus.map(_.sku(productId))
  }
}

case class SkuVo(id: Option[String], name: String, description: Option[String], listPrice: BigDecimal, salePrice: Option[BigDecimal], saleStartDate: Option[String], saleEndDate: Option[String]) {
  def sku(productId: String) = {
    Sku(id.getOrElse(LocalIdGenerator.generateSkuId()), name, description, None, productId, listPrice, salePrice, AppHelper.convertDateFromText(saleStartDate), AppHelper.convertDateFromText(saleEndDate), new Timestamp(new java.util.Date().getTime))
  }
}

object ProductVo {
  private val log = Logger(this.getClass)
  def apply(product: Product): ProductVo = {
    val childSkus: Seq[SkuVo] = product.childSkus.map(SkuVo(_))
    val categories = product.categories
    var catIds = ""
    var catNames = ""
    for (cat <- categories) {
      catIds = catIds + "," + cat.id
      catNames = catNames + "," + cat.name
    }
    if (catIds != "") {
      catIds = catIds.substring(1)
      catNames = catNames.substring(1)
    }
    if(log.isDebugEnabled)
      log.debug("inventory value is: " + product.inventory.get.dailyUpdate  + " stock:" + product.inventory.get.stock)
    val inventoryTuple = product.inventory match {
      case Some(inv) =>
        (Some(inv.dailyUpdate), Some(inv.stock))
      case _ =>
        (None, None)
    }
    val vo = ProductVo(Some(product.id), product.merchantId, product.name, product.description, product.longDescription, AppHelper.convertDateToText(product.startDate), AppHelper.convertDateToText(product.endDate), catIds, catNames, inventoryTuple._1, inventoryTuple._2,childSkus)
    vo.imageUrl = product.imageUrl
    vo
  }
}

object SkuVo {
  def apply(sku: Sku): SkuVo = {
    SkuVo(Some(sku.id), sku.name, sku.description, sku.listPrice, sku.salePrice, AppHelper.convertDateToText(sku.saleStartDate), AppHelper.convertDateToText(sku.saleEndDate))
  }
}
