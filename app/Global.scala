
import actors.OrderProcessActor
import actors.OrderProcessActor.Start
import akka.actor.Props
import models.CategoryCategory
import java.sql.Date
import models._
import play.api.db.DB
import play.api.GlobalSettings
import play.api.libs.concurrent.Akka
import play.Logger
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global

// Use H2Driver to connect to an H2 database

import scala.slick.driver.MySQLDriver.simple._

import play.api.Play.current
import play.api.Application

/**
 * Created with IntelliJ IDEA.
 * User: simonwang
 * Date: 5/9/13
 * Time: 11:52 PM
 * To change this template use File | Settings | File Templates.
 */
object Global extends GlobalSettings {

  private def initializeTable() {
    lazy val database = Database.forDataSource(DB.getDataSource())
    database.withSession {
      implicit session =>
      //Initialize Profile Tables
      //Profiles.ddl.create
      //Merchants.ddl.create
      //      Products.ddl.drop
      //      Products.ddl.create
      //Skus.ddl.drop
      //      Skus.ddl.create
      //MerchantAdvInfos.ddl.create
      //      Addresses.ddl.create
      //  UserAddresses.ddl.drop
      //      UserAddresses.ddl.create
      //ProductCategories.ddl.create
      //      Categories.ddl.create
      //      CategoryCategories.ddl.drop
      //      InternalUsers.ddl.create
      //      Areas.ddl.create
      //      MerchantServiceInfos.ddl.create
      //      MerchantShippingScopes.ddl.create
      //      TableQuery[OrderRepo].ddl.create
      //      TableQuery[CommerceItemRepo].ddl.create
      //      TableQuery[PriceInfoRepo].ddl.create
      //      TableQuery[PaymentGroupRepo].ddl.create
      //      TableQuery[ShippingGroupRepo].ddl.create
      //        TableQuery[IdGenerationRepo].ddl.create
      //        TableQuery[PromotionBannerRepo].ddl.create
      //        TableQuery[PromotionBannerItemRepo].ddl.create
      //        TableQuery[ProductSalesVolumeRepo].ddl.create
      //              TableQuery[ProfileCreditPointsRepo].ddl.create
//        TableQuery[CreditPointsDetailRepo].ddl.create
              TableQuery[InteractiveEventRepo].ddl.create
              TableQuery[InteractiveEventReplyRepo].ddl.create
      //      CategoryCategories.ddl.create

      /*if (Query(Profiles).list().isEmpty){
        println("Simon is emtpty yet")
        Profiles.insert(Profile(IdGenerator.generateProfileId(), "simonwang@gmail.com", "test", "simon", Some("M"), Some(new Date(1981 + 1900, 9, 20))))
      }*/

      /*val categoryQuery = TableQuery[Categories]
      val catcatQuery = TableQuery[CategoryCategories]
      if(categoryQuery.list().isEmpty){
        println("insert categories")
        categoryQuery.insert(Category("cat1", "工作餐", "工作餐，简单快捷", "", false))
        categoryQuery.insert(Category("cat11", "套餐", "套餐，包含饮料", "", true))
        categoryQuery.insert(Category("cat12", "炒饭", "我是蛋炒饭", "", true))
        categoryQuery.insert(Category("cat13", "面食", "这里有各种各样的面食", "", true))
        categoryQuery.insert(Category("cat2", "私房菜", "个人绝密私房菜", "", false))
        categoryQuery.insert(Category("cat21", "小吃", "金牌小吃", "", true))
        categoryQuery.insert(Category("cat22", "营养套餐", "营养套餐，个人单炒", "", false))
        catcatQuery.insert(CategoryCategory("cat1", "cat11"))
        catcatQuery.insert(CategoryCategory("cat1", "cat12"))
        catcatQuery.insert(CategoryCategory("cat1", "cat13"))
        catcatQuery.insert(CategoryCategory("cat2", "cat21"))
        catcatQuery.insert(CategoryCategory("cat2", "cat22"))
        categoryQuery.insert(Category("cat111", "套餐11", "套餐，包含饮料", "", false))
        categoryQuery.insert(Category("cat112", "炒饭11", "我是蛋炒饭"c, "", false))
        categoryQuery.insert(Category("cat113", "面食11", "这里有各种各样的面食", "", false))
        catcatQuery.insert(CategoryCategory("cat11", "cat111"))
        catcatQuery.insert(CategoryCategory("cat11", "cat112"))
        catcatQuery.insert(CategoryCategory("cat11", "cat113"))
      }*/
    }
  }

  override def onStart(app: Application) {
    println("start loading")
    //    println("leaves:" + Area.allLeaveAreas())
//            initializeTable()
    startBackendProcess(app)
  }

  def startBackendProcess(app: Application) = {
    val orderProcessActor = Akka.system.actorOf(Props(new OrderProcessActor()))
    Akka.system.scheduler.schedule(0 seconds, 1 minutes, orderProcessActor, Start)
    val promoBannerActor = Akka.system.actorOf(Props())
  }
}
