package models

import scala.slick.driver.H2Driver.simple._
import java.sql.Date
import play.api.db.DB
import play.api.Play.current
import slick.session.Database
import Database.threadLocalSession
import scala.Predef._
import helper.DBHelper


/**
 * Created with IntelliJ IDEA.
 * User: simonwang
 * Date: 5/10/13
 * Time: 3:34 PM
 * To change this template use File | Settings | File Templates.
 */

case class Profile(id: String, login: String, password: String, name: String, gender: Option[String], birthDay: Option[Date])

object Profiles extends Table[Profile]("user") {
  def id = column[String]("id", O.PrimaryKey)

  def login = column[String]("login")

  def password = column[String]("password")

  def name = column[String]("name")

  def gender = column[Option[String]]("gender")

  def birthday = column[Option[Date]]("birthday")

  def * = id ~ login ~ password ~ name ~ gender ~ birthday <>(
    (id, login, password, name, gender, birthday) => Profile(id, login, password, name, gender, birthday),
    (p: Profile) => Some(p.id, p.login, p.password, p.name, p.gender, p.birthDay)
    )
}

object Profile {
  def createUser(user: Profile) = DBHelper.database.withTransaction {
    Profiles.insert(user)
  }

  def updateUser(user: Profile) = DBHelper.database.withTransaction {
    Profiles.where(_.id === user.id).update(user)
  }

  def authenticateUser(login: String, password: String) = {
    DBHelper.database.withSession {
      val result = for (p <- Profiles if (p.login === login && p.password === password)) yield p
      result.firstOption()
    }
  }

  def findUserByLogin(login: String): Option[Profile] = DBHelper.database.withSession {
    val result = for (p <- Profiles if (p.login === login)) yield p
    result.firstOption()
  }

  def findAllUsers() = DBHelper.database.withSession {
    val result = for (p <- Profiles) yield p
    result.list()
  }
}

/*object ProfileService {
  def authenticateUser(login:String, password:String) = {
    ModelConfiguration.database.withSession{
      val result = for(p <- Profiles if(p.login === login && p.password === password)) yield p
      result.firstOption()
    }
  }



  def createUser(user:Profile) = ModelConfiguration.database.withTransaction{
    println("start creating users")
    val s = Profiles.insert(user);
    println("user id is :" + user.id + " " + s)
  }

  def findAllUsers() = ModelConfiguration.database.withSession{
    val result = for(p <- Profiles) yield p
    result.list()
  }*/

