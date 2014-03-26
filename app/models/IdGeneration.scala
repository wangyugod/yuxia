package models

import scala.slick.driver.MySQLDriver.simple._
import util.{DBHelper}
import scala.slick.lifted
import java.util.UUID
import play.api.Logger

/**
 * Created by thinkpad-pc on 14-3-23.
 */
case class IdGeneration(id: String, initial: Int, step: Int, sequence: Int, prefix: String, key: String) {
  lazy val generatePassword = prefix + (sequence - 1)
}

class IdGenerationRepo(tag: Tag) extends Table[IdGeneration](tag, "id_gen") {
  val id = column[String]("id")
  val initial = column[Int]("initial")
  val step = column[Int]("step")
  val sequence = column[Int]("value")
  val prefix = column[String]("prefix")
  val key = column[String]("key")

  def * = (id, initial, step, sequence, prefix, key) <>(IdGeneration.tupled, IdGeneration.unapply)
}

object IdGeneration extends ((String, Int, Int, Int, String, String) => IdGeneration) {
  val log = Logger(this.getClass)
  val SHIPPING_GROUP = "shipping_group"
  val ORDER = "order"
  val PROFILE = "profile"
  val PRODUCT = "product"
  val CATEGORY = "category"
  val SKU = "sku"
  val ADDRESS = "address"
  val AREA = "area"
  val MERCHANT = "merchant"
  val COMMERCE_ITEM = "commerce_item"
  val PRICE = "price"
  val PAYMENT_GROUP = "payment_group"
  val INITIAL = 100000
  val STEP = 1
  val prefixMap = Map(SHIPPING_GROUP -> "sg", ORDER -> "o", PROFILE -> "p", PRODUCT -> "pd", CATEGORY -> "cat", SKU -> "sku", ADDRESS -> "addr", AREA -> "area", MERCHANT -> "mc", COMMERCE_ITEM -> "ci", PRICE -> "pi", PAYMENT_GROUP -> "pg")

  def getNextId(key: String) = DBHelper.database.withTransaction {
    implicit session =>
      val idGenQuery = TableQuery[IdGenerationRepo]
      idGenQuery.where(_.key === key).firstOption match {
        case Some(idGenerator) =>
          val nextGen = idGenerator.copy(sequence = idGenerator.sequence + idGenerator.step)
          if(log.isDebugEnabled)
            log.debug("new password is " + nextGen.generatePassword)
          idGenQuery.where(_.id === idGenerator.id).update(nextGen)
          nextGen.generatePassword
        case _ =>
          val idGeneration: IdGeneration = IdGeneration(UUID.randomUUID().toString, INITIAL, STEP, INITIAL + STEP, prefixMap.get(key).get, key)
          idGenQuery.insert(idGeneration)
          idGeneration.generatePassword
      }
  }
}

trait IdGenerator {
  def generateId(): String
}

object ProfileIdGenerator extends IdGenerator {
  override def generateId() = this.synchronized {
    IdGeneration.getNextId(IdGeneration.PROFILE)
  }
}

object OrderIdGenerator extends IdGenerator {
  override def generateId() = this.synchronized {
    IdGeneration.getNextId(IdGeneration.ORDER)
  }
}

object ShippingGroupIdGenerator extends IdGenerator {
  override def generateId() = this.synchronized {
    IdGeneration.getNextId(IdGeneration.SHIPPING_GROUP)
  }
}

object ProductIdGenerator extends IdGenerator {
  override def generateId() = this.synchronized {
    IdGeneration.getNextId(IdGeneration.PRODUCT)
  }
}

object CategoryIdGenerator extends IdGenerator {
  override def generateId() = this.synchronized {
    IdGeneration.getNextId(IdGeneration.CATEGORY)
  }
}

object SkuIdGenerator extends IdGenerator {
  override def generateId() = this.synchronized {
    IdGeneration.getNextId(IdGeneration.SKU)
  }
}

object AddressIdGenerator extends IdGenerator {
  override def generateId() = this.synchronized {
    IdGeneration.getNextId(IdGeneration.ADDRESS)
  }
}

object AreaIdGenerator extends IdGenerator {
  override def generateId() = this.synchronized {
    IdGeneration.getNextId(IdGeneration.AREA)
  }
}

object MerchantIdGenerator extends IdGenerator {
  override def generateId() = this.synchronized {
    IdGeneration.getNextId(IdGeneration.MERCHANT)
  }
}

object CommerceItemIdGenerator extends IdGenerator {
  override def generateId() = this.synchronized {
    IdGeneration.getNextId(IdGeneration.COMMERCE_ITEM)
  }
}

object PriceIdGenerator extends IdGenerator {
  override def generateId() = this.synchronized {
    IdGeneration.getNextId(IdGeneration.PRICE)
  }
}

object PaymentGroupIdGenerator extends IdGenerator {
  override def generateId() = this.synchronized {
    IdGeneration.getNextId(IdGeneration.PAYMENT_GROUP)
  }
}

object LocalIdGenerator {

  def generateProfileId() = {
    ProfileIdGenerator.generateId()
  }

  def generateMerchantId() = {
    MerchantIdGenerator.generateId()
  }

  def generateProductId() = {
    ProductIdGenerator.generateId()
  }

  def generateCategoryId() = {
    CategoryIdGenerator.generateId()
  }

  def generateSkuId() = {
    SkuIdGenerator.generateId()
  }

  def generateAddressId() = {
    AddressIdGenerator.generateId()
  }

  def generateAreaId() = {
    AreaIdGenerator.generateId()
  }

  def generateOrderId() = {
    OrderIdGenerator.generateId()
  }

  def generateCommerceItemId() = {
    CommerceItemIdGenerator.generateId()
  }

  def generatePriceInfoId() = {
    PriceIdGenerator.generateId()
  }

  def generateShippingGroupId() = {
    ShippingGroupIdGenerator.generateId()
  }

  def generatePaymentGroupId() = {
    PaymentGroupIdGenerator.generateId()
  }
}