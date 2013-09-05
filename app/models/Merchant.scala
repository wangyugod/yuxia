package models

import util.DBHelper
import scala.Predef._
import play.api.db.DB
import play.api.Play.current
import slick.session.Database
import Database.threadLocalSession
import scala.slick.driver.MySQLDriver.simple._

/**
 * Created with IntelliJ IDEA.
 * User: simonwang
 * Date: 6/26/13
 * Time: 5:17 PM
 * To change this template use File | Settings | File Templates.
 */
case class Merchant(id: String, login: String, password: String, name: String, description: Option[String], registeredBy: String) {
  lazy val advancedInfo = DBHelper.database.withSession {
    Query(MerchantAdvInfos).where(_.id === this.id).firstOption
  }
}

case class MerchantAdvInfo(id: String, merchNum: Option[String], artificialPerson: String, artPerCert: String, addressId: String, phoneNum: String){
  lazy val address = DBHelper.database.withSession{
    Query(Addresses).where(_.id === this.addressId).first()
  }
}


object Merchants extends Table[Merchant]("merchant") {
  def id = column[String]("id", O.PrimaryKey)

  def login = column[String]("login")

  def password = column[String]("password")

  def name = column[String]("name")

  def description = column[Option[String]]("description")

  def registeredBy = column[String]("reg_by")

  def * = id ~ login ~ password ~ name ~ description ~ registeredBy <>(
    (id, login, password, name, description, registeredBy) => Merchant(id, login, password, name, description, registeredBy),
    (p: Merchant) => Some(p.id, p.login, p.password, p.name, p.description, p.registeredBy)
    )
}

object MerchantAdvInfos extends Table[MerchantAdvInfo]("merchant_adv_info") {
  def id = column[String]("id", O.PrimaryKey)

  def merchNum = column[Option[String]]("merch_num")

  def artificialPerson = column[String]("artificial_per")

  def artPerCert = column[String]("art_per_cert")

  def addressId = column[String]("address_id")

  def phoneNum = column[String]("phone_num")

  def * = id ~ merchNum ~ artificialPerson ~ artPerCert ~ addressId ~ phoneNum <> (
    (id, merchNum, artificialPerson, artPerCert, addressId, phoneNum) => MerchantAdvInfo(id, merchNum, artificialPerson, artPerCert, addressId, phoneNum),
    (mai: MerchantAdvInfo) => Some(mai.id, mai.merchNum, mai.artificialPerson, mai.artPerCert, mai.addressId, mai.phoneNum)
    )
}

object Merchant {
  def authenticateUser(login: String, password: String): Option[Merchant] = {
    DBHelper.database.withSession {
      val result = for (p <- Merchants if (p.login === login && p.password === password)) yield p
      result.firstOption()
    }
  }

  def create(merchant: Merchant) = {
    DBHelper.database.withTransaction {
      Merchants.insert(merchant)
    }
  }

  def findByLogin(login: String): Option[Merchant] = {
    DBHelper.database.withSession {
      Query(Merchants).where(_.login === login).firstOption
    }
  }
}


