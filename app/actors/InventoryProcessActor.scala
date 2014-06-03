package actors

import akka.actor.Actor
import util.DBHelper
import models.Product
import play.api.Logger

/**
 * Created by thinkpad-pc on 14-5-27.
 */

case class UpdateInventory(items: List[(String, Int)])

case class GetInventory(productId: String)

class InventoryProcessActor extends Actor {
  private val log = Logger(this.getClass)
  val UPDATE_INVENTOR_SUCCESS = "update_inventory_success"
  val STOCK_NOT_ENOUGH = "stock_not_enough"
  val INVALID_MESSAGE = "invalid_message"

  def receive = {
    case update: UpdateInventory =>
      if (log.isDebugEnabled)
        log.debug(s"update inventory message $update")
      var badItemList = List.empty[String]
      DBHelper.database.withTransaction {
        implicit session =>
          update.items.foreach {
            item =>
              val currentStock = Product.findProductInventory(item._1)
              if (currentStock - item._2 < 0) {
                badItemList = item._1 :: badItemList
              }
          }
          if (badItemList.isEmpty)
            update.items.foreach {
              item =>
                Product.updateInventory(item._1, item._2)
            }
          sender ! badItemList
      }
    case _ =>
      if (log.isDebugEnabled)
        log.debug("invalid message received")
      sender ! INVALID_MESSAGE
  }
}
