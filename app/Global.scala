
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
      Products.ddl.drop
      Products.ddl.create
      ProductCategories.ddl.create
      Categories.ddl.create

      /*if (Query(Profiles).list().isEmpty){
        println("Simon is emtpty yet")
        Profiles.insert(Profile(IdGenerator.generateProfileId(), "simonwang@gmail.com", "test", "simon", Some("M"), Some(new Date(1981 + 1900, 9, 20))))
      }*/
    }
  }

  override def onStart(app: Application) {
    println("start loading")
//    initializeTable()
  }
}
