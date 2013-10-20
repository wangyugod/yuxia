package vo

import models.Address

/**
 * Created with IntelliJ IDEA.
 * User: simonwang
 * Date: 10/20/13
 * Time: 10:54 PM
 * To change this template use File | Settings | File Templates.
 */
case class AddressVo(id: String, province: String, city: String, district: String, contactPhone: String, addressLine: String, contactPerson: String, areaId: Option[String], isDefaultAddress: Option[Boolean]) {
  lazy val address = Address(id: String, province: String, city: String, district: String, contactPhone: String, addressLine: String, contactPerson: String, areaId: Option[String])
}

object AddressVo {
  def apply(address: Address): AddressVo = AddressVo(address.id, address.province, address.city, address.district, address.contactPhone, address.addressLine, address.contactPerson, address.areaId, address.isDefault)
}
