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

  def generateProfileId() = {
    println("generating ProfileID now")
    PROFILE_PREFIX + UUID.randomUUID()
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

}
