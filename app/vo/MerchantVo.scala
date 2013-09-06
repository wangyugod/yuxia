package vo

import models._
import util.{DBHelper, IdGenerator}

/**
 * Created with IntelliJ IDEA.
 * User: simonwang
 * Date: 9/5/13
 * Time: 9:31 PM
 * To change this template use File | Settings | File Templates.
 */
case class MerchantVo(id: Option[String], login: String, name: String, description: Option[String], merchNum: Option[String], artificialPerson: String, artPerCert: String, phoneNum: String, addressId:Option[String], province: String, city: String, district: String, contactPhone: String, addressLine: String, contactPerson: String) {
  lazy val addrId: String = {
    this.addressId match {
      case Some(aid) => aid
      case _ => IdGenerator.generateAddressId()
    }
  }

  lazy val merchant: Merchant = {
    val persistMerch = Merchant.findByLogin(this.id.get)
    Merchant(id.get, login, persistMerch.get.password, name, description, persistMerch.get.registeredBy)
  }

  lazy val merchantAdvInfo: MerchantAdvInfo = {
    MerchantAdvInfo(id.get, merchNum, artificialPerson, artPerCert, phoneNum, addrId)
  }

  lazy val address: Address = {
    Address(addrId, province, city, district, contactPhone, addressLine, contactPerson)
  }

}

object MerchantVo {
  def apply(merchant: Merchant): MerchantVo = DBHelper.database.withSession{
    val advMerchant = merchant.advancedInfo
    advMerchant match {
      case Some(am) => {
        val address = am.address
        MerchantVo(Some(merchant.id), merchant.login, merchant.name, merchant.description, am.merchNum, am.artificialPerson, am.artPerCert, am.phoneNum, Some(am.addressId), address.province, address.city, address.district, address.contactPhone, address.addressLine, address.contactPerson)
      }
      case _ =>{
        MerchantVo(Some(merchant.id), merchant.login, merchant.name, merchant.description, None, "", "", "", None, "", "", "", "", "", "")
      }
    }
  }
}
