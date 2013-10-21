package models

import scala.slick.driver.MySQLDriver.simple._
import slick.session.Database
import Database.threadLocalSession
import java.sql.Timestamp
import util.DBHelper
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
    parentAreaId match {
      case Some(parentId) => Area.findById(parentId)
      case _ => None
    }
  }

  lazy val isRoot = parentAreaId.isEmpty
}

object Areas extends Table[Area]("area") {
  def id = column[String]("id", O.PrimaryKey)

  def name = column[String]("name")

  def detail = column[String]("detail")

  def parentAreaId = column[Option[String]]("parent_area_id")

  def lastUpdatedTime = column[Timestamp]("last_updated_time")

  def * = id ~ name ~ detail ~ parentAreaId ~ lastUpdatedTime <>(
    (id, name, detail, parentAreaId, lastUpdateTime) => Area(id, name, detail, parentAreaId, lastUpdateTime),
    (area: Area) => Some(area.id, area.name, area.detail, area.parentAreaId, area.lastUpdatedTime)
    )
}


case class Address(id: String, province: String, city: String, district: String, contactPhone: String, addressLine: String, contactPerson: String, areaId: Option[String]) {
  lazy val area = DBHelper.database.withSession {
    areaId match {
      case Some(id) => Area.findById(id)
      case _ => None
    }
  }

  lazy val isDefault = DBHelper.database.withSession {
    Query(UserAddresses).where(_.addrId === id).firstOption match {
      case Some(ua) => ua.isDefault
      case _ => None
    }
  }
}

object Addresses extends Table[Address]("address") {
  def id = column[String]("id", O.PrimaryKey)

  def province = column[String]("province")

  def city = column[String]("city")

  def district = column[String]("district")

  def phone = column[String]("phone")

  def addressLine = column[String]("address_line")

  def contactPerson = column[String]("receiver_name")

  def areaId = column[Option[String]]("area_id")

  def * = id ~ province ~ city ~ district ~ phone ~ addressLine ~ contactPerson ~ areaId <>(
    (id, province, city, district, phone, addressLine, contactPerson, areaId) => Address(id, province, city, district, phone, addressLine, contactPerson, areaId),
    (addr: Address) => Some(addr.id, addr.province, addr.city, addr.district, addr.contactPhone, addr.addressLine, addr.contactPerson, addr.areaId)
    )
}

case class UserAddress(userId: String, addressId: String, isDefault: Option[Boolean])

object UserAddresses extends Table[UserAddress]("user_addr") {
  def userId = column[String]("user_id")

  def addrId = column[String]("address_id")

  def isDefault = column[Option[Boolean]]("is_default")

  def pk = primaryKey("user_addr_pk", (userId, addrId))

  def * = userId ~ addrId ~ isDefault <>(UserAddress, UserAddress.unapply(_))
}

object Area {
  def all() = DBHelper.database.withSession {
    Query(Areas).list()
  }

  def rootAreas() = DBHelper.database.withSession {
    Query(Areas).where(_.parentAreaId isNull).list()
  }

  def findById(id: String) = DBHelper.database.withSession {
    Query(Areas).where(_.id === id).firstOption
  }

  def findByIds(ids: Seq[String]) = DBHelper.database.withSession {
    Query(Areas).where(_.id inSetBind (ids)).list()
  }

  def saveOrUpdate(area: Area) = DBHelper.database.withTransaction {
    findById(area.id) match {
      case Some(a) => Query(Areas).where(_.id === area.id).update(area)
      case _ => Areas.insert(area)
    }
  }

  def childAreas(id: String) = DBHelper.database.withSession {
    Query(Areas).where(_.parentAreaId === id).list()
  }

  def delete(id: String) = DBHelper.database.withTransaction {
    Query(Areas).where(_.id === id).delete
  }
}

object Address {
  private val log = Logger(this.getClass())

  def userAddresses(userId: String) = DBHelper.database.withSession {
    val addIds = for (ua <- UserAddresses if ua.userId === userId) yield ua.addrId
    val addressIds = addIds.list()
    if (log.isDebugEnabled)
      log.debug("found addressId :" + addressIds)
    Query(Addresses).where(_.id inSetBind (addressIds)).list()
  }

  def findById(id: String) = DBHelper.database.withSession {
    Query(Addresses).where(_.id === id).firstOption
  }

  def saveOrUpdate(userId: String, address: Address, isDefaultAddress: Boolean) = DBHelper.database.withTransaction {
    val userAddresses = Query(UserAddresses).where(_.userId === userId).list()
    findById(address.id) match {
      case Some(addr) => {
        Query(Addresses).where(_.id === address.id).update(address)
        val originalUA = Query(UserAddresses).where(_.userId === userId).where(_.addrId === address.id)
        if (isDefaultAddress && !originalUA.first().isDefault.getOrElse(false)) {
          val originalDefaultAddress = Query(UserAddresses).where(_.userId === userId).where(_.isDefault === true)
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
        Addresses.insert(address)
        if (userAddresses.isEmpty) {
          if (log.isDebugEnabled)
            log.debug("This is the first address of customer, set it to default address")
          UserAddresses.insert(UserAddress(userId, address.id, Some(true)))
        } else if (isDefaultAddress) {
          if (log.isDebugEnabled)
            log.debug("set current inserted address as default address, and make original non-default")
          val originalDefaultAddress = Query(UserAddresses).where(_.userId === userId).where(_.isDefault === true)
          if (originalDefaultAddress.firstOption.isDefined) {
            originalDefaultAddress.update(UserAddress(userId, originalDefaultAddress.firstOption.get.addressId, Some(false)))
          }
          UserAddresses.insert(UserAddress(userId, address.id, Some(true)))
        } else {
          if (log.isDebugEnabled)
            log.debug("not default address!")
          UserAddresses.insert(UserAddress(userId, address.id, Some(false)))
        }
      }
    }
  }

  def deleteUserAddress(userId: String, addressId: String) = DBHelper.database.withTransaction {
    Query(UserAddresses).where(_.userId === userId).where(_.addrId === addressId).delete
    Query(Addresses).where(_.id === addressId).delete
  }
}





