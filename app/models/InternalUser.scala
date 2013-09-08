package models

import scala.slick.driver.MySQLDriver.simple._
import util.DBHelper
import slick.session.Database
import Database.threadLocalSession


/**
 * Created with IntelliJ IDEA.
 * User: simonwang
 * Date: 9/8/13
 * Time: 11:03 AM
 * To change this template use File | Settings | File Templates.
 */
case class InternalUser(id: String, login: String, password: String, name: String)

object InternalUsers extends Table[InternalUser]("internal_user") {
  def id = column[String]("id", O.PrimaryKey)

  def login = column[String]("login")

  def password = column[String]("password")

  def name = column[String]("name")

  def * = id ~ login ~ password ~ name <>(
    (id, login, password, name) => InternalUser(id, login, password, name),
    (user: InternalUser) => Some(user.id, user.login, user.password, user.name)
    )
}


object InternalUser{
  def authenticateUser(login: String, password: String) = {
    DBHelper.database.withSession {
      val result = for (p <- InternalUsers if (p.login === login && p.password === password)) yield p
      result.firstOption()
    }
  }

  def findById(id: String) = DBHelper.database.withSession{
    Query(InternalUsers).where(_.id === id).firstOption
  }
}