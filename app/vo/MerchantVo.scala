package vo

import models._
import util.{DBHelper, IdGenerator}
import play.api.Logger

/**
 * Created with IntelliJ IDEA.
 * User: simonwang
 * Date: 9/5/13
 * Time: 9:31 PM
 * To change this template use File | Settings | File Templates.
 */
case class MerchantVo(id: Option[String], login: String, name: String, description: Option[String], merchNum: Option[String], artificialPerson: String, artPerCert: String, addressId: Option[String], province: String, city: String, district: String, contactPhone: String, addressLine: String, contactPerson: String) {
  private val log = Logger(this.getClass())
  lazy val addrId: String = {
    this.addressId match {
      case Some(aid) => aid
      case _ => IdGenerator.generateAddressId()
    }
  }

  lazy val merchant: Merchant = {
    if (log.isDebugEnabled)
      log.debug("current merchant id is " + id)
    val persistMerch = Merchant.findById(this.id.get)
    Merchant(id.get, login, persistMerch.get.password, name, description, persistMerch.get.registeredBy)
  }

  lazy val merchantAdvInfo: MerchantAdvInfo = {
    MerchantAdvInfo(id.get, merchNum, artificialPerson, artPerCert, addrId)
  }

  lazy val address: Address = {
    Address(addrId, province, city, district, contactPhone, addressLine, contactPerson, None)
  }

}

object MerchantVo {
  def apply(merchant: Merchant): MerchantVo = DBHelper.database.withSession {
    val advMerchant = merchant.advancedInfo
    advMerchant match {
      case Some(am) => {
        val address = am.address
        MerchantVo(Some(merchant.id), merchant.login, merchant.name, merchant.description, am.merchNum, am.artificialPerson, am.artPerCert, Some(am.addressId), address.province, address.city, address.district, address.contactPhone, address.addressLine, address.contactPerson)
      }
      case _ => {
        MerchantVo(Some(merchant.id), merchant.login, merchant.name, merchant.description, None, "", "", None, "", "", "", "", "", "")
      }
    }
  }
}


case class MerchantServiceVo(id: String, startTime: Double, endTime: Double, areaIds: Seq[String]) {

  def merchantServiceInfo =
    MerchantServiceInfo(id, startTime, endTime)

  def merchantShippingInfos: Seq[MerchantShippingScope] = areaIds.map(MerchantShippingScope(id, _))
}

object MerchantServiceVo {
  def apply(serviceInfo: MerchantServiceInfo): MerchantServiceVo = {
    MerchantServiceVo(serviceInfo.id, serviceInfo.startTime, serviceInfo.endTime, serviceInfo.areas.map(_.id))
  }
}
