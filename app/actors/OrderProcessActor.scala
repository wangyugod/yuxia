package actors

import akka.actor.{ActorRef, Props, Actor}
import models.{ProfileCreditPoints, OrderProcessFlag, OrderState, Order}
import play.api.libs.concurrent.Akka
import play.api.Logger
import play.api.Play.current
import util.DBHelper
import actors.OrderProcessActor.{ProfilePointsFlag, ProductCounterFlag}

/**
 * Created by thinkpad-pc on 14-4-17.
 */

object OrderProcessActor {

  case object Start

  case object ProductCounterFlag

  case object ProfilePointsFlag

}

class OrderProcessActor extends Actor {

  import OrderProcessActor._

  val orderFlagActor = context.actorOf(Props[OrderFlagActor], name = "orderFlagActor")
  val productSalesCounter = context.actorOf(Props(new ProductSalesCounter(orderFlagActor)), name = "productSalesCounter")
  val profileCreditPointsCalculator = context.actorOf(Props(new ProfileCreditPointsCalculator(orderFlagActor)), name = "profileCreditPointsCalculator")

  private val log = Logger(this.getClass)


  def receive = {
    case Start =>
      if (log.isDebugEnabled)
        log.debug("Order Process get Start Message")
      DBHelper.database.withSession {
        implicit session =>
          val orders = Order.fetchOrderToBeProcessed(OrderState.SUBMITTED)
          for (order <- orders) {
            if (order.processFlag != OrderProcessFlag.PRODUCT_COUNT_FLAG)
              productSalesCounter ! order
            if (order.processFlag != OrderProcessFlag.PROFILE_POINT_FLAG)
              //Send to profile points actor
              profileCreditPointsCalculator ! order
              println("Send to Profile Points Calculation")
          }
      }
    case _ =>
  }
}

class ProfileCreditPointsCalculator(orderFlagActor: ActorRef) extends Actor {
  private val log = Logger(this.getClass)

  import OrderProcessActor._

  def receive = {
    case order: Order =>
      if (log.isDebugEnabled)
        log.debug("receive order for ProfileCredit Calculation: " + order.id)
      DBHelper.database.withTransaction {
        implicit session =>
          ProfileCreditPoints.increaseCreditPointsByOrder(order)
      }
      orderFlagActor !(ProfilePointsFlag, order)
    case _ =>
      println("invalid message")
  }

}

class ProductSalesCounter(orderFlagActor: ActorRef) extends Actor {
  private val log = Logger(this.getClass)

  import OrderProcessActor._

  def receive = {
    case order: Order =>
      if (log.isDebugEnabled)
        log.debug("order id:" + order.id)
      DBHelper.database.withTransaction {
        implicit session =>
          Order.countProduct(order)
      }
      orderFlagActor !(ProductCounterFlag, order)
    case _ =>
      println("unknown message")

  }


}

class OrderFlagActor extends Actor {
  private val log = Logger(this.getClass)

  def receive = {
    case (ProductCounterFlag, order: Order) =>
      if (log.isDebugEnabled)
        log.debug("received message for product counter flag, update order " + order.id + " process flag")
      DBHelper.database.withTransaction {
        implicit session =>
          Order.updateOrder(order.copy(processFlag = order.processFlag + OrderProcessFlag.PRODUCT_COUNT_FLAG))
      }
    case (ProfilePointsFlag, order: Order) =>
      if (log.isDebugEnabled)
        log.debug("received message for profile points flag, update order " + order.id + " process flag")
      DBHelper.database.withTransaction {
        implicit session =>
          Order.updateOrder(order.copy(processFlag = order.processFlag + OrderProcessFlag.PROFILE_POINT_FLAG))
      }
    case _ => println("Invalid message")
  }
}



