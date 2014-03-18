package models

import scala.slick.driver.MySQLDriver.simple._
import slick.jdbc.{GetResult, StaticQuery => Q}
import java.sql.Timestamp
import util.{AddressInfo, DBHelper}
import play.api.Logger

/**
 * Created with IntelliJ IDEA.
 * User: simonwang
 * Date: 6/26/13
 * Time: 5:31 PM
 * To change this template use File | Settings | File Templates.
 */

case class Area(id: String, name: String, detail: String, parentAreaId: Option[String], lastUpdatedTime: Timestamp) {
  lazy val parentArea = DBHelper.database.withSession {
    implicit session =>
      parentAreaId match {
        case Some(parentId) => Area.findById(parentId)
        case _ => None
      }
  }

  lazy val isRoot = parentAreaId.isEmpty
}

class Areas(tag: Tag) extends Table[Area](tag, "area") {
  def id = column[String]("id", O.PrimaryKey)

  def name = column[String]("name")

  def detail = column[String]("detail")

  def parentAreaId = column[Option[String]]("parent_area_id")

  def lastUpdatedTime = column[Timestamp]("last_updated_time")

  def * = (id, name, detail, parentAreaId, lastUpdatedTime) <>(Area.tupled, Area.unapply)
}


case class Address(id: String, province: String, city: String, district: String, contactPhone: String, addressLine: String, contactPerson: String, areaId: Option[String]) {
  lazy val area = DBHelper.database.withSession {
    implicit session =>
      areaId match {
        case Some(id) => Area.findById(id)
        case _ => None
      }
  }

  lazy val isDefault = DBHelper.database.withSession {
    implicit session =>
      TableQuery[UserAddresses].where(_.addrId === id).firstOption match {
        case Some(ua) => ua.isDefault
        case _ => None
      }
  }

  override def toString = AddressInfo.fullName(province, city, district) + addressLine
}

class Addresses(tag: Tag) extends Table[Address](tag, "address") {
  def id = column[String]("id", O.PrimaryKey)

  def province = column[String]("province")

  def city = column[String]("city")

  def district = column[String]("district")

  def phone = column[String]("phone")

  def addressLine = column[String]("address_line")

  def contactPerson = column[String]("receiver_name")

  def areaId = column[Option[String]]("area_id")

  def * = (id, province, city, district, phone, addressLine, contactPerson, areaId) <>(Address.tupled, Address.unapply)
}

case class UserAddress(userId: String, addressId: String, isDefault: Option[Boolean])

class UserAddresses(tag: Tag) extends Table[UserAddress](tag, "user_addr") {
  def userId = column[String]("user_id")

  def addrId = column[String]("address_id")

  def isDefault = column[Option[Boolean]]("is_default")

  def pk = primaryKey("user_addr_pk", (userId, addrId))

  def * = (userId, addrId, isDefault) <>(UserAddress.tupled, UserAddress.unapply)
}

object Area extends ((String, String, String, Option[String], Timestamp) => Area) {
  private val log = Logger(this.getClass())
  implicit val getAreaResult = GetResult(r => Area(r.<<, r.<<, r.<<, r.<<, r.<<))

  def all() = DBHelper.database.withSession {
    implicit session =>
      TableQuery[Areas].list()
  }

  def rootAreas() = DBHelper.database.withSession {
    implicit session =>
      TableQuery[Areas].where(_.parentAreaId isNull).list()
  }

  def findById(id: String) = DBHelper.database.withSession {
    implicit session =>
      TableQuery[Areas].where(_.id === id).firstOption
  }

  def findByIds(ids: Seq[String]) = DBHelper.database.withSession {
    implicit session =>
      TableQuery[Areas].where(_.id inSetBind (ids)).list()
  }

  def saveOrUpdate(area: Area) = DBHelper.database.withTransaction {
    implicit session =>
      findById(area.id) match {
        case Some(a) => TableQuery[Areas].where(_.id === area.id).update(area)
        case _ => TableQuery[Areas].insert(area)
      }
  }

  def allLeaveAreas() = DBHelper.database.withSession {
    implicit session =>
      val result = Q.queryNA[Area]("select * from area a where not exists (select * from area b where b.parent_area_id  = a.id)").list()
      if (log.isDebugEnabled) {
        log.debug("leave areas is:" + result)
      }
      result
  }

  def childAreas(id: String) = DBHelper.database.withSession {
    implicit session =>
      TableQuery[Areas].where(_.parentAreaId === id).list()
  }

  def delete(id: String) = DBHelper.database.withTransaction {
    implicit session =>
      TableQuery[Areas].where(_.id === id).delete
  }
}

object Address extends ((String, String, String, String, String, String, String, Option[String]) => Address) {
  private val log = Logger(this.getClass())
  private val addressQuery = TableQuery[Addresses]
  private val userAddressQuery = TableQuery[UserAddresses]

  def userAddresses(userId: String) = DBHelper.database.withSession {
    implicit session =>
      val addIds = for (ua <- userAddressQuery if ua.userId === userId) yield ua.addrId
      val addressIds = addIds.list()
      if (log.isDebugEnabled)
        log.debug("found addressId :" + addressIds)
      addressQuery.where(_.id inSetBind (addressIds)).list()
  }

  def findById(id: String)(implicit session: Session) = {
    addressQuery.where(_.id === id).firstOption
  }

  def saveOrUpdate(userId: String, address: Address, isDefaultAddress: Boolean)(implicit session: Session) = {
    val userAddresses = userAddressQuery.where(_.userId === userId).list()
    findById(address.id) match {
      case Some(addr) => {
        addressQuery.where(_.id === address.id).update(address)
        val originalUA = userAddressQuery.where(_.userId === userId).where(_.addrId === address.id)
        if (isDefaultAddress && !originalUA.first().isDefault.getOrElse(false)) {
          val originalDefaultAddress = userAddressQuery.where(_.userId === userId).where(_.isDefault === true)
          if (log.isDebugEnabled)
            log.debug("Should update current address to default address, and update original default to non default")
          if (originalDefaultAddress.firstOption.isDefined)
            originalDefaultAddress.update(UserAddress(userId, originalDefaultAddress.first.addressId, Some(false)))
          originalUA.update(UserAddress(userId, address.id, Some(true)))
        } else if (!isDefaultAddress && originalUA.first().isDefault.get) {
          if (log.isDebugEnabled)
            log.debug("change current default address to non default")
          originalUA.update(UserAddress(userId, address.id, Some(false)))
        }
      }
      case _ => {
        addressQuery.insert(address)
        if (userAddresses.isEmpty) {
          if (log.isDebugEnabled)
            log.debug("This is the first address of customer, set it to default address")
          userAddressQuery.insert(UserAddress(userId, address.id, Some(true)))
        } else if (isDefaultAddress) {
          if (log.isDebugEnabled)
            log.debug("set current inserted address as default address, and make original non-default")
          val originalDefaultAddress = userAddressQuery.where(_.userId === userId).where(_.isDefault === true)
          if (originalDefaultAddress.firstOption.isDefined) {
            originalDefaultAddress.update(UserAddress(userId, originalDefaultAddress.firstOption.get.addressId, Some(false)))
          }
          userAddressQuery.insert(UserAddress(userId, address.id, Some(true)))
        } else {
          if (log.isDebugEnabled)
            log.debug("not default address!")
          userAddressQuery.insert(UserAddress(userId, address.id, Some(false)))
        }
      }
    }
  }

  def deleteUserAddress(userId: String, addressId: String)(implicit session: Session) = {
    userAddressQuery.where(_.userId === userId).where(_.addrId === addressId).delete
    addressQuery.where(_.id === addressId).delete
  }
}





