package models

import java.sql.Date

/**
 * Created with IntelliJ IDEA.
 * User: simonwang
 * Date: 6/26/13
 * Time: 4:47 PM
 * To change this template use File | Settings | File Templates.
 */
case class Category(id:String, name:String, description:String, longDescription:String)

case class Product(id:String, name:String, description:String, longDescription:String, startDate:Date, endDate:Date, merchantId:String, categoryId:String)

case class Sku(id:String, name:String, description:String, longDescription:String, size:Int, productId:String)

case class Media(id:String, name:String, description:String, url:String)


