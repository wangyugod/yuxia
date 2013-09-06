
import util.IdGenerator
import java.sql.Date
import models.{Profile, Profiles}
import models._
import play.api.db.DB
import play.api.GlobalSettings

// Use H2Driver to connect to an H2 database

import scala.slick.driver.MySQLDriver.simple._

import play.api.Play.current
import play.api.Application
import slick.session.Database

import Database.threadLocalSession

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
      //Initialize Profile Tables
      //Profiles.ddl.create
      //Merchants.ddl.create
//      Products.ddl.drop
//      Products.ddl.create
      //Skus.ddl.drop
//      Skus.ddl.create
      MerchantAdvInfos.ddl.create
      //ProductCategories.ddl.create
//      Categories.ddl.create
//      CategoryCategories.ddl.drop

//      CategoryCategories.ddl.create

      /*if (Query(Profiles).list().isEmpty){
        println("Simon is emtpty yet")
        Profiles.insert(Profile(IdGenerator.generateProfileId(), "simonwang@gmail.com", "test", "simon", Some("M"), Some(new Date(1981 + 1900, 9, 20))))
      }*/

      if(Query(Categories).list().isEmpty){
        println("insert categories")
        Categories.insert(Category("cat1", "工作餐", "工作餐，简单快捷", "", false))
        Categories.insert(Category("cat11", "套餐", "套餐，包含饮料", "", true))
        Categories.insert(Category("cat12", "炒饭", "我是蛋炒饭", "", true))
        Categories.insert(Category("cat13", "面食", "这里有各种各样的面食", "", true))
        Categories.insert(Category("cat2", "私房菜", "个人绝密私房菜", "", false))
        Categories.insert(Category("cat21", "小吃", "金牌小吃", "", true))
        Categories.insert(Category("cat22", "营养套餐", "营养套餐，个人单炒", "", false))
        CategoryCategories.insert(CategoryCategory("cat1", "cat11"))
        CategoryCategories.insert(CategoryCategory("cat1", "cat12"))
        CategoryCategories.insert(CategoryCategory("cat1", "cat13"))
        CategoryCategories.insert(CategoryCategory("cat2", "cat21"))
        CategoryCategories.insert(CategoryCategory("cat2", "cat22"))
        Categories.insert(Category("cat111", "套餐11", "套餐，包含饮料", "", false))
        Categories.insert(Category("cat112", "炒饭11", "我是蛋炒饭", "", false))
        Categories.insert(Category("cat113", "面食11", "这里有各种各样的面食", "", false))
        CategoryCategories.insert(CategoryCategory("cat11", "cat111"))
        CategoryCategories.insert(CategoryCategory("cat11", "cat112"))
        CategoryCategories.insert(CategoryCategory("cat11", "cat113"))
      }
    }
  }

  override def onStart(app: Application) {
    println("start loading")

//    initializeTable()
  }
}
