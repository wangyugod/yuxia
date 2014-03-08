package models

import util.DBHelper
import scala.Predef._
import play.api.db.DB
import play.api.Play.current
import scala.slick.driver.MySQLDriver.simple._
import play.api.Logger

/**
 * Created with IntelliJ IDEA.
 * User: simonwang
 * Date: 6/26/13
 * Time: 5:17 PM
 * To change this template use File | Settings | File Templates.
 */
case class Merchant(id: String, login: String, password: String, name: String, description: Option[String], registeredBy: String) {
  lazy val advancedInfo = DBHelper.database.withSession {
    implicit session =>
      TableQuery[MerchantAdvInfos].where(_.id === this.id).firstOption
  }

  lazy val serviceInfo = DBHelper.database.withSession {
    implicit session =>
      TableQuery[MerchantServiceInfos].where(_.id === id).firstOption
  }
}

case class MerchantAdvInfo(id: String, merchNum: Option[String], artificialPerson: String, artPerCert: String, addressId: String) {
  lazy val address = DBHelper.database.withSession {
    implicit session =>
      TableQuery[Addresses].where(_.id === this.addressId).first()
  }
}

case class MerchantServiceInfo(id: String, startTime: Double, endTime: Double) {
  lazy val areas: List[Area] = DBHelper.database.withSession {
    implicit session =>
      val scopeList = TableQuery[MerchantShippingScopes].where(_.merchantId === id).list()
      scopeList match {
        case Nil => Nil
        case list => {
          val areaIds = for (scope <- list) yield scope.areaId
          TableQuery[Areas].where(_.id inSetBind (areaIds)).list()
        }
      }
  }
}

case class MerchantShippingScope(merchantId: String, areaId: String)


class Merchants(tag: Tag) extends Table[Merchant](tag, "merchant") {
  def id = column[String]("id", O.PrimaryKey)

  def login = column[String]("login")

  def password = column[String]("password")

  def name = column[String]("name")

  def description = column[Option[String]]("description")

  def registeredBy = column[String]("reg_by")

  def * = (id, login, password, name, description, registeredBy) <>(Merchant.tupled, Merchant.unapply)
}

class MerchantAdvInfos(tag: Tag) extends Table[MerchantAdvInfo](tag, "merchant_adv_info") {
  def id = column[String]("id", O.PrimaryKey)

  def merchNum = column[Option[String]]("merch_num")

  def artificialPerson = column[String]("artificial_per")

  def artPerCert = column[String]("art_per_cert")

  def addressId = column[String]("address_id")

  def * = (id, merchNum, artificialPerson, artPerCert, addressId) <>(MerchantAdvInfo.tupled, MerchantAdvInfo.unapply)
}

class MerchantServiceInfos(tag: Tag) extends Table[MerchantServiceInfo](tag, "merchant_serv_info") {
  def id = column[String]("id", O.PrimaryKey)

  def startTime = column[Double]("start_time")

  def endTime = column[Double]("end_time")

  def * = (id, startTime, endTime) <>(MerchantServiceInfo.tupled, MerchantServiceInfo.unapply)
}

class MerchantShippingScopes(tag: Tag) extends Table[MerchantShippingScope](tag, "merchant_ship_scope") {
  def merchantId = column[String]("merch_id")

  def areaId = column[String]("area_id")

  def * = (merchantId, areaId) <>(MerchantShippingScope.tupled, MerchantShippingScope.unapply)

  def pk = primaryKey("merch_ship_scope_pk", (merchantId, areaId))
}

object Merchant extends ((String, String, String, String, Option[String], String) => Merchant) {
  def authenticateUser(login: String, password: String): Option[Merchant] = DBHelper.database.withSession {
    implicit session =>
      val result = for (p <- TableQuery[Merchants] if (p.login === login && p.password === password)) yield p
      result.firstOption()
  }


  def create(merchant: Merchant) = DBHelper.database.withTransaction {
    implicit session =>
      TableQuery[Merchants].insert(merchant)
  }

  def findByLogin(login: String): Option[Merchant] = DBHelper.database.withSession {
    implicit session =>
      TableQuery[Merchants].where(_.login === login).firstOption
  }


  def findById(id: String): Option[Merchant] = DBHelper.database.withSession {
    implicit session =>
      TableQuery[Merchants].where(_.id === id).firstOption
  }


  def updateMerchantInfo(merchant: Merchant, advMerchInfo: MerchantAdvInfo, address: Address) = DBHelper.database.withTransaction {
    implicit session =>
      val existedMerchant = findById(merchant.id).get
      TableQuery[Merchants].where(_.id === merchant.id).update(merchant)
      existedMerchant.advancedInfo match {
        case Some(adv) => {
          TableQuery[MerchantAdvInfos].where(_.id === advMerchInfo.id).update(advMerchInfo)
          TableQuery[Addresses].where(_.id === address.id).update(address)
        }
        case None => {
          TableQuery[MerchantAdvInfos].insert(advMerchInfo)
          TableQuery[Addresses].insert(address)
        }
      }
  }
}

object MerchantServiceInfo extends ((String, Double, Double) => MerchantServiceInfo) {
  private val log = Logger(this.getClass)

  def findById(id: String) = DBHelper.database.withSession {
    implicit session =>
    TableQuery[MerchantServiceInfos].where(_.id === id).firstOption
  }

  def updateMerchantServiceInfo(serviceInfo: MerchantServiceInfo, areaIds: Seq[String]) = DBHelper.database.withTransaction {
    implicit session =>
    TableQuery[MerchantServiceInfos].where(_.id === serviceInfo.id).firstOption match {
      case Some(si) => {
        TableQuery[MerchantServiceInfos].where(_.id === serviceInfo.id).update(serviceInfo)
        val existedAreaIds: List[String] = si.areas.map(_.id)
        if (existedAreaIds != areaIds.toList) {
          if (log.isDebugEnabled) {
            log.debug("Scope changed, needs to update")
          }
         TableQuery[MerchantShippingScopes].where(_.merchantId === serviceInfo.id).delete
          for (areaId <- areaIds) {
            TableQuery[MerchantShippingScopes].insert(MerchantShippingScope(serviceInfo.id, areaId))
          }
        }
      }
      case _ => {
        TableQuery[MerchantServiceInfos].insert(serviceInfo)
        for (areaId <- areaIds) {
          TableQuery[MerchantShippingScopes].insert(MerchantShippingScope(serviceInfo.id, areaId))
        }
      }
    }
  }


}


