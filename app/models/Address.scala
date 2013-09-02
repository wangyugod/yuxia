package models

import scala.slick.driver.MySQLDriver.simple._
import slick.session.Database
import Database.threadLocalSession

/**
 * Created with IntelliJ IDEA.
 * User: simonwang
 * Date: 6/26/13
 * Time: 5:31 PM
 * To change this template use File | Settings | File Templates.
 */

case class Area(id:String, name: String, description: String, parentAreaId: String)


case class Address(id: String, province: String, city: String, district: String, contactPhone: String, addressLine: String, contactPerson: String)

object Addresses extends Table[Address]("address") {
  def id = column[String]("id", O.PrimaryKey)
  def province = column[String]("province")
  def city = column[String]("city")
  def district = column[String]("district")
  def phone = column[String]("phone")
  def addressLine = column[String]("address_line")
  def contactPerson = column[String]("receiver_name")
  def * = id ~ province ~ city ~ district ~ phone ~ addressLine ~ contactPerson <> (
    (id, province, city, district, phone, addressLine, contactPerson) => Address(id, province, city, district, phone, addressLine, contactPerson),
    (addr: Address) => Some(addr.id, addr.province, addr.city, addr.district, addr.contactPhone, addr.addressLine, addr.contactPerson)
    )
}

case class UserAddress(userId: String, addressId: String)

object UserAddresses extends Table[UserAddress]("user_addr"){
  def userId = column[String]("user_id")
  def addrId = column[String]("address_id")
  def pk = primaryKey("user_addr_pk", (userId, addrId))
  def * = userId ~ addrId <> (UserAddress, UserAddress.unapply(_))
}






