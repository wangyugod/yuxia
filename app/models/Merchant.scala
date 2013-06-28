package models

import helper.DBHelper
import scala.Predef._
import scala.slick.driver.H2Driver.simple._

/**
 * Created with IntelliJ IDEA.
 * User: simonwang
 * Date: 6/26/13
 * Time: 5:17 PM
 * To change this template use File | Settings | File Templates.
 */
case class Merchant(id:String, login:String, password:String, merchantNum:String, name:String)


object Merchants extends Table[Merchant]("merchant"){
  def id = column[String]("id",O.PrimaryKey)
  def login = column[String]("login")
  def password = column[String]("password")
  def merchantNum = column[String]("merchantNum")
  def name = column[String]("name")
  def * = id ~ login ~ password ~ merchantNum ~ name <> (
    (id, login, password, merchantNum, name) => Merchant(id, login, password, merchantNum, name),
    (p:Merchant) => Some(p.id, p.login, p.password, p.merchantNum, p.name)
    )
}




object Merchant{
  def authenticateUser(login:String, password:String) = {
    DBHelper.database.withSession{
      val result = for(p <- Merchants if(p.login === login && p.password === password)) yield p
      result.firstOption()
    }
  }
}


