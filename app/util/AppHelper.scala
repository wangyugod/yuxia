package util

import java.sql.Date
import play.api.Play
import play.api.Play._
import play.api.i18n.Messages

import models.Product
import play.api.mvc._
import play.api._

/**
 * Created with IntelliJ IDEA.
 * User: simonwang
 * Date: 6/14/13
 * Time: 4:46 PM
 * To change this template use File | Settings | File Templates.
 */
object AppHelper {

  def convertDateFromText(date: Option[String]) = date match {
    case (Some(x)) => {
      val DATE_FORMAT = "yyyy-MM-dd"
      val sdf = new java.text.SimpleDateFormat(DATE_FORMAT)
      val d = sdf.parse(x)
      Some(new Date(d.getTime))
    }
    case _ => None
  }

  def convertDateToText(birthday: Option[Date]): Option[String] = birthday match {
    case Some(date) => {
      val DATE_FORMAT = "yyyy-MM-dd"
      val sdf = new java.text.SimpleDateFormat(DATE_FORMAT, java.util.Locale.CHINESE);
      Some(sdf.format(date))
    }
    case None => None
  }

  def checkCurrentTime(startTime: Double, endTime: Double) = {
    val format = "HH:mm"
    val sdf = new java.text.SimpleDateFormat(format)
    val currentTimeString = sdf.format(new java.util.Date())
    val time = currentTimeString.split("\\:")
    val currentTime = time(0).toDouble + time(1).toDouble / 60
    currentTime <= endTime
  }

  def productImageDir = {
    Play.application.path.getPath + "\\" + Play.current.configuration.getString("prod.image.dir").get
  }

  def promoImageDir = {
    Play.application.path.getPath + "\\" + Play.current.configuration.getString("promo.image.dir").get
  }

  def displayShipTime(startTime: Double, endTime: Double) = {
    processTime(startTime.toString) + "--" + processTime(endTime.toString)
  }

  private def processTime(time: String) = {
    val start = time.split("\\.")
    if (start.length == 1)
      start(0) + ":00"
    else
      start(0) + ":" + (if (start(1).equals("5")) "30" else "00")
  }

  def displayPrice(priceRange: (BigDecimal, BigDecimal)) = {
    priceRange match {
      case (x, y) if x == y => Messages("srp.price.single", x)
      case (x, y) => {
        Messages("srp.price.range", x.setScale(2).toString(), y.setScale(2).toString())
      }
    }
  }

  def productImage(product: Product) = {
    controllers.routes.Assets.at("images/product/" + product.imageUrl)
  }

  def promoImage(imageName: String) = {
    controllers.routes.Assets.at("images/promo/" + imageName)
  }

  def maskMail(email: String) = {
    val user = email.substring(0, email.indexOf('@'))
    val domain = email.substring(email.indexOf('@'))
    var userMask = ""
    val userStart = user.charAt(0)
    if (user.length < 3) {
      userMask = userStart + "*"
    } else {
      userMask = userStart + "*****" + user.charAt(user.length - 1)
    }
    userMask + domain
  }
}
