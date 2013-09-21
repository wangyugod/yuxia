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

  lazy val serviceInfo = DBHelper.database.withSession{
    Query(MerchantServiceInfos).where(_.id === id).firstOption
  }
}

case class MerchantAdvInfo(id: String, merchNum: Option[String], artificialPerson: String, artPerCert: String, addressId: String) {
  lazy val address = DBHelper.database.withSession {
    Query(Addresses).where(_.id === this.addressId).first()
  }
}

case class MerchantServiceInfo(id: String, startTime: String, endTime: String) {
  lazy val areas: List[Area] = DBHelper.database.withSession {
    val scopeList = Query(MerchantShippingScopes).where(_.merchantId === id).list()
    scopeList match {
      case Nil => Nil
      case list => {
        val areaIds = for (scope <- list) yield scope.areaId
        Query(Areas).where(_.id inSetBind(areaIds)).list()
      }
    }
  }
}

case class MerchantShippingScope(merchantId: String, areaId: String)


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

  def * = id ~ merchNum ~ artificialPerson ~ artPerCert ~ addressId <>(
    (id, merchNum, artificialPerson, artPerCert, addressId) => MerchantAdvInfo(id, merchNum, artificialPerson, artPerCert, addressId),
    (mai: MerchantAdvInfo) => Some(mai.id, mai.merchNum, mai.artificialPerson, mai.artPerCert, mai.addressId)
    )
}

object MerchantServiceInfos extends Table[MerchantServiceInfo]("merchant_serv_info") {
  def id = column[String]("id", O.PrimaryKey)

  def startTime = column[String]("start_time")

  def endTime = column[String]("end_time")

  def * = id ~ startTime ~ endTime <>(
    (id, startTime, endTime) => MerchantServiceInfo(id, startTime, endTime),
    (msi: MerchantServiceInfo) => Some(msi.id, msi.startTime, msi.endTime)
    )
}

object MerchantShippingScopes extends Table[MerchantShippingScope]("merchant_ship_scope") {
  def merchantId = column[String]("merch_id")

  def areaId = column[String]("area_id")

  def * = merchantId ~ areaId <>(MerchantShippingScope, MerchantShippingScope.unapply(_))

  def pk = primaryKey("merch_ship_scope_pk", (merchantId, areaId))
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

  def findByLogin(login: String): Option[Merchant] = DBHelper.database.withSession {
    Query(Merchants).where(_.login === login).firstOption
  }


  def findById(id: String): Option[Merchant] = DBHelper.database.withSession {
    Query(Merchants).where(_.id === id).firstOption
  }


  def updateMerchantInfo(merchant: Merchant, advMerchInfo: MerchantAdvInfo, address: Address) = DBHelper.database.withTransaction {
    val existedMerchant = findById(merchant.id).get
    Query(Merchants).where(_.id === merchant.id).update(merchant)
    existedMerchant.advancedInfo match {
      case Some(adv) => {
        Query(MerchantAdvInfos).where(_.id === advMerchInfo.id).update(advMerchInfo)
        Query(Addresses).where(_.id === address.id).update(address)
      }
      case None => {
        MerchantAdvInfos.insert(advMerchInfo)
        Addresses.insert(address)
      }
    }
  }
}


