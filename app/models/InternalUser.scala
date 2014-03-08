package models

import scala.slick.driver.MySQLDriver.simple._
import util.DBHelper

/**
 * Created with IntelliJ IDEA.
 * User: simonwang
 * Date: 9/8/13
 * Time: 11:03 AM
 * To change this template use File | Settings | File Templates.
 */
case class InternalUser(id: String, login: String, password: String, name: String)

class InternalUsers(tag: Tag) extends Table[InternalUser](tag, "internal_user") {
  def id = column[String]("id", O.PrimaryKey)

  def login = column[String]("login")

  def password = column[String]("password")

  def name = column[String]("name")

  def * = (id, login, password, name) <>(InternalUser.tupled, InternalUser.unapply)
}

object InternalUser extends ((String, String, String, String) => InternalUser) {
  def authenticateUser(login: String, password: String) = DBHelper.database.withSession {
    implicit session =>
      val result = for (p <- TableQuery[InternalUsers] if (p.login === login && p.password === password)) yield p
      result.firstOption()
  }


  def findById(id: String) = DBHelper.database.withSession {
    implicit session =>
      TableQuery[InternalUsers].where(_.id === id).firstOption
  }
}