package models

import java.sql.Timestamp

/**
 * Created with IntelliJ IDEA.
 * User: simonwang
 * Date: 10/30/13
 * Time: 11:08 PM
 * To change this template use File | Settings | File Templates.
 */
case class Order(id: String, profileId: String, state: Int, orderPriceId: String, createdTime: Timestamp, modifiedTime: Timestamp) {

}

case class CommerceItem(id: String, skuId: String, orderId: String, priceId: String, createdTime: Timestamp)

case class PriceInfo(id: String, listPrice: BigDecimal, realPrice: BigDecimal, promoDescription: Option[String])

case class PaymentGroup(id: String, name: String, amount: BigDecimal, orderId: String)

case class ShippingGroup(id:String, address: String, shippingMethod: String)
