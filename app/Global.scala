
import helper.IdGenerator
import java.sql.Date
import models.{Profile, Profiles}
import models._
import play.api.db.DB
import play.api.GlobalSettings

// Use H2Driver to connect to an H2 database

import scala.slick.driver.H2Driver.simple._

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
      //Products.ddl.drop
      //Products.ddl.create
      //ProductCategories.ddl.create
      Categories.ddl.create
      CategoryCategories.ddl.create

      /*if (Query(Profiles).list().isEmpty){
        println("Simon is emtpty yet")
        Profiles.insert(Profile(IdGenerator.generateProfileId(), "simonwang@gmail.com", "test", "simon", Some("M"), Some(new Date(1981 + 1900, 9, 20))))
      }*/

      if(Query(Categories).list().isEmpty){
        Categories.insert(Category("cat1", "工作餐", "工作餐，简单快捷", ""))
        Categories.insert(Category("cat11", "套餐", "套餐，包含饮料", ""))
        Categories.insert(Category("cat12", "炒饭", "我是蛋炒饭", ""))
        Categories.insert(Category("cat13", "面食", "这里有各种各样的面食", ""))
        Categories.insert(Category("cat2", "私房菜", "个人绝密私房菜", ""))
        Categories.insert(Category("cat21", "小吃", "金牌小吃", ""))
        Categories.insert(Category("cat22", "营养套餐", "营养套餐，个人单炒", ""))
        CategoryCategories.insert(CategoryCategory("cat1", "cat11"))
        CategoryCategories.insert(CategoryCategory("cat1", "cat12"))
        CategoryCategories.insert(CategoryCategory("cat1", "cat13"))
        CategoryCategories.insert(CategoryCategory("cat2", "cat21"))
        CategoryCategories.insert(CategoryCategory("cat2", "cat22"))
      }
    }
  }

  override def onStart(app: Application) {
    println("start loading")
//    initializeTable()
  }
}
