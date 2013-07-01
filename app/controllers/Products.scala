package controllers

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import validation.Constraints
import views.html
import models._
import helper._

/**
 * Created with IntelliJ IDEA.
 * User: simonwang
 * Date: 7/1/13
 * Time: 8:00 PM
 * To change this template use File | Settings | File Templates.
 */
object Products extends Controller with Merchants {

  val productForm = Form(
    mapping(
      "id" -> optional(text),
      "merchantId" -> text,
      "name" -> nonEmptyText,
      "description" -> nonEmptyText,
      "longDescription" -> nonEmptyText,
      "startDate" -> text,
      "endDate" -> text,
      "categories" -> text
    )((id, merchantId, name, description, longDescription, startDate, endDate, categories) => (Product(id.getOrElse(IdGenerator.generateProfileId()), name, description, longDescription, AppHelper.convertBirthdayFromText(Some(startDate)).get, AppHelper.convertBirthdayFromText(Some(endDate)).get, merchantId, ""), categories))
      ((x:(Product,String)) => Some(Some(x._1.id), x._1.merchantId, x._1.name, x._1.description, x._1.longDescription, AppHelper.convertBirthdayToText(Some(x._1.startDate)).get, AppHelper.convertBirthdayToText(Some(x._1.endDate)).get, x._2))
  )

  def newProduct = Action {
    implicit request => {
      Ok(html.merchandise.product)
    }
  }

  def create = Action {
    implicit request => {
      Ok("Create Successfully")
    }
  }


}
