package util

import java.util.UUID

/**
 * Created with IntelliJ IDEA.
 * User: simonwang
 * Date: 5/27/13
 * Time: 6:40 PM
 * To change this template use File | Settings | File Templates.
 */
object IdGenerator {

  val PROFILE_PREFIX = "p"
  val ACCT_PREFIX = "a"
  val TAG_PREFIX = "t"
  val PRODUCT_PREFIX = "pd"
  val CATEGORY_PREFIX = "cat"
  val SKU_PREFIX = "sku"
  val ADDRESS_PREFIX = "addr"
  val AREA_PREFIX = "area"
  val MERCHANT_PREFIX = "mc"
  val ORDER_PREFIX = "o"
  val COMMERCEITEM_PREFIX = "ci"
  val PRICE_INFO_PREFIX = "pi"
  val SHIPPING_GROUP_PREFIX = "sg"
  val PAYMENT_GROUP_PREFIX = "pg"

  def generateProfileId() = {
    println("generating ProfileID now")
    PROFILE_PREFIX + UUID.randomUUID()
  }

  def generateMerchantId() = {
    MERCHANT_PREFIX + UUID.randomUUID()
  }



  def generateAccountingId() = {
    ACCT_PREFIX + UUID.randomUUID()
  }

  def generateTagId() = {
    TAG_PREFIX + UUID.randomUUID()
  }

  def generateProductId() = {
    println("generating ProfileID now")
    PRODUCT_PREFIX + UUID.randomUUID()
  }

  def generateCategoryId() = {
    CATEGORY_PREFIX + UUID.randomUUID()
  }

  def generateSkuId() = {
    SKU_PREFIX + UUID.randomUUID()
  }

  def generateAddressId() = {
    ADDRESS_PREFIX + UUID.randomUUID()
  }

  def generateAreaId() = {
    AREA_PREFIX + UUID.randomUUID()
  }

  def generateOrderId() = {
    ORDER_PREFIX + UUID.randomUUID()
  }

  def generateCommerceItemId() = {
    COMMERCEITEM_PREFIX + UUID.randomUUID()
  }

  def generatePriceInfoId() = {
    PRICE_INFO_PREFIX + UUID.randomUUID()
  }

  def generateShippingGroupId() = {
    SHIPPING_GROUP_PREFIX  + UUID.randomUUID()
  }

  def generatePaymentGroupId() = {
    PAYMENT_GROUP_PREFIX  + UUID.randomUUID()
  }
}
