package actors

import akka.actor.Actor

/**
 * Created by thinkpad-pc on 14-5-18.
 */

object PromotionBannerActor {
  case object TopSellerCalculate
}

class PromotionBannerActor extends Actor{
  import PromotionBannerActor._

  def receive = {
    case TopSellerCalculate =>

    case _ =>
  }

}
