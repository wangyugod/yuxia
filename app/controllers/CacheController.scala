package controllers

import play.api.cache.Cache
import models.{Product, Sku}
import play.api.Play
import play.api.Play.current
import scala.slick.driver.MySQLDriver.simple._

/**
 * Created by thinkpad-pc on 14-5-28.
 */
trait CacheController {

  def getSku(id: String)(implicit session: Session) = Cache.getAs[Sku](id) match {
    case Some(sku) => sku
    case _ =>
      val s = Product.findSkuById(id).get
      Cache.set(s.id, s, Play.current.configuration.getInt("cache.ttl").getOrElse(5))
      s
  }

  def getProduct(id: String)(implicit session: Session): Product = Cache.getAs[Product](id) match {
    case Some(product) => product
    case _ =>
      val product = Product.findById(id).get
      Cache.set(product.id, product, Play.current.configuration.getInt("cache.ttl").getOrElse(5))
      product
  }


}
