package models

import scala.slick.driver.MySQLDriver.simple._
import java.sql.Date
import play.api.db.DB
import play.api.Play.current
import scala.Predef._
import util.DBHelper
import play.api.{Logger, Play}


/**
 * Created with IntelliJ IDEA.
 * User: simonwang
 * Date: 5/10/13
 * Time: 3:34 PM
 * To change this template use File | Settings | File Templates.
 */

case class Profile(id: String, login: String, password: String, name: String, gender: Option[String], birthDay: Option[Date]) {
  lazy val defaultAddress: Option[Address] = DBHelper.database.withSession {
    implicit session =>
      Profile.findDefaultAddress(id)
  }

  lazy val addresses = DBHelper.database.withSession {
    implicit session =>
      Profile.findUserAddresses(id)
  }

  lazy val defaultArea: Option[Area] = defaultAddress match {
    case Some(address) => Area.findById(address.areaId.get)
    case _ => None
  }

  lazy val currentOrder: Option[Order] = Profile.findCurrentOrder(id)

}

class Profiles(tag: Tag) extends Table[Profile](tag, "user") {
  def id = column[String]("id", O.PrimaryKey)

  def login = column[String]("login")

  def password = column[String]("password")

  def name = column[String]("name")

  def gender = column[Option[String]]("gender")

  def birthday = column[Option[Date]]("birthday")

  def * = (id, login, password, name, gender, birthday) <>(Profile.tupled, Profile.unapply)
}

object Profile extends ((String, String, String, String, Option[String], Option[Date]) => Profile) {
  private val profileQuery = TableQuery[Profiles]

  def createUser(user: Profile) = DBHelper.database.withTransaction {
    implicit session =>
      profileQuery.insert(user)
  }

  def updateUser(user: Profile) = DBHelper.database.withTransaction {
    implicit session =>
      profileQuery.where(_.id === user.id).update(user)
  }

  def authenticateUser(login: String, password: String) = DBHelper.database.withSession {
    implicit session =>
      val result = for (p <- profileQuery if (p.login === login && p.password === password)) yield p
      result.firstOption()
  }

  def findUserByLogin(login: String): Option[Profile] = DBHelper.database.withSession {
    implicit session =>
      val result = for (p <- profileQuery if (p.login === login)) yield p
      result.firstOption()
  }

  def findUserById(id: String)(implicit session: Session): Option[Profile] = {
    profileQuery.where(_.id === id).firstOption
  }

  def findAllUsers() = DBHelper.database.withSession {
    implicit session =>
      val result = for (p <- profileQuery) yield p
      result.list()
  }

  def findCurrentOrder(profileId: String) = DBHelper.database.withSession {
    implicit session =>
      TableQuery[OrderRepo].where(_.profileId === profileId).where(_.state === OrderState.INITIAL).firstOption
  }

  def findUserAddresses(userId: String)(implicit session: Session) = {
    val userAddressQuery = TableQuery[UserAddresses]
    val addrsIds = (userAddressQuery.filter(_.userId === userId).map(_.addrId).list())
    TableQuery[Addresses].where(_.id inSetBind addrsIds).list()
  }

  def findDefaultAddress(userId: String)(implicit session: Session) = {
    val userAddressQuery = TableQuery[UserAddresses]
    userAddressQuery.where(_.userId === userId).where(_.isDefault === true).firstOption match {
      case Some(ua) => Address.findById(ua.addressId)
      case _ => {
        userAddressQuery.where(_.userId === userId).firstOption match {
          case Some(userAddr) => Address.findById(userAddr.addressId)
          case _ => None
        }
      }
    }
  }

  def findUserOrders(userId: String)(implicit session: Session) = {
    TableQuery[OrderRepo].filter(_.profileId === userId).filter(_.state > OrderState.INITIAL).list()
  }
}