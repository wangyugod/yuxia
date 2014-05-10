package models

import scala.slick.driver.MySQLDriver.simple._
import java.sql.{Timestamp, Date}
import play.api.db.DB
import play.api.Play.current
import scala.Predef._
import util.{Encoder, DBHelper}
import play.api.{Logger, Play}
import java.math.MathContext
import play.api.i18n.Messages


/**
 * Created with IntelliJ IDEA.
 * User: simonwang
 * Date: 5/10/13
 * Time: 3:34 PM
 * To change this template use File | Settings | File Templates.
 */

case class Profile(id: String, login: String, password: String, pwdSalt: String, name: String, gender: Option[String], birthDay: Option[Date]) {
  lazy val defaultAddress: Option[Address] = DBHelper.database.withSession {
    implicit session =>
      Profile.findDefaultAddress(id)
  }

  lazy val userCreditPoints = DBHelper.database.withSession {
    implicit session =>
      ProfileCreditPoints.findUserCreditPoints(this.id)
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

  def pwdSalt = column[String]("pwd_salt")

  def name = column[String]("name")

  def gender = column[Option[String]]("gender")

  def birthday = column[Option[Date]]("birthday")

  def * = (id, login, password, pwdSalt, name, gender, birthday) <>(Profile.tupled, Profile.unapply)
}

object Profile extends ((String, String, String, String, String, Option[String], Option[Date]) => Profile) {
  private val profileQuery = TableQuery[Profiles]

  def createUser(user: Profile) = DBHelper.database.withTransaction {
    implicit session =>
      user.copy(password = Encoder.encodeSHA(user.password + user.pwdSalt))
      profileQuery.insert(user)
  }

  def updateUser(user: Profile) = DBHelper.database.withTransaction {
    implicit session =>
      profileQuery.where(_.id === user.id).update(user)
  }

  def authenticateUser(login: String, password: String) = DBHelper.database.withSession {
    implicit session =>
      val result = for (p <- profileQuery if (p.login === login)) yield p
      result.firstOption() match {
        case Some(user) if (Encoder.encodeSHA(password + user.pwdSalt) == user.password) =>
          Some(user)
        case _ =>
          None
      }
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

  def updateProfilePassword(userId: String, password: String)(implicit session: Session) = {
    val user = Profile.findUserById(userId).get
    val newPassword = Encoder.encodeSHA(password + user.pwdSalt)
    profileQuery.where(_.id === userId).update(user.copy(password = newPassword))
  }

  def findUserOrders(userId: String)(implicit session: Session) = {
    TableQuery[OrderRepo].filter(_.profileId === userId).filter(_.state > OrderState.INITIAL).list()
  }
}

case class ProfileCreditPoints(userId: String, creditPoints: Int, lastModifiedTime: Timestamp)

case class CreditPointsDetail(id: String, userId: String, creditPoints: Int, description: String, modifiedTime: Timestamp)

class ProfileCreditPointsRepo(tag: Tag) extends Table[ProfileCreditPoints](tag, "user_credit") {
  def userId = column[String]("user_id", O.PrimaryKey)

  def creditPoints = column[Int]("credit_points")

  def lastModifiedTime = column[Timestamp]("last_modified_time")

  def * = (userId, creditPoints, lastModifiedTime) <>(ProfileCreditPoints.tupled, ProfileCreditPoints.unapply)
}

class CreditPointsDetailRepo(tag: Tag) extends Table[CreditPointsDetail](tag, "user_credit_detail") {
  def id = column[String]("id", O.PrimaryKey)

  def userId = column[String]("user_id")

  def creditPoints = column[Int]("credit_points")

  def description = column[String]("description")

  def modifiedTime = column[Timestamp]("modified_time")

  def * = (id, userId, creditPoints, description, modifiedTime) <>(CreditPointsDetail.tupled, CreditPointsDetail.unapply)
}

object ProfileCreditPoints extends ((String, Int, Timestamp) => ProfileCreditPoints) {
  private val log = Logger(this.getClass)

  def increaseCreditPointsByOrder(order: Order)(implicit session: Session) = {
    val userId = order.profileId
    val creditPoints = calculatePoints(order)
    if (log.isDebugEnabled)
      log.debug(s"add points $creditPoints for order $order.id")
    findUserCreditPoints(userId) match {
      case Some(userCreditPoints) =>
        val userCredit = userCreditPoints.copy(creditPoints = userCreditPoints.creditPoints + creditPoints, lastModifiedTime = new Timestamp(new java.util.Date().getTime))
        TableQuery[ProfileCreditPointsRepo].where(_.userId === userId).update(userCredit)
      case _ =>
        TableQuery[ProfileCreditPointsRepo].insert(ProfileCreditPoints(userId, creditPoints, new Timestamp(new java.util.Date().getTime)))
    }
    TableQuery[CreditPointsDetailRepo].insert(CreditPointsDetail(LocalIdGenerator.generateUserCreditDetailId(), userId, creditPoints, Messages("user.credit.detail", order.id), new Timestamp(new java.util.Date().getTime)))
  }

  /**
   * Currently 1 yuan means 1 point
   * @param order
   * @return
   */
  def calculatePoints(order: Order) = {
    order.priceInfo.actualPrice.intValue
  }

  def findUserCreditPoints(userId: String)(implicit session: Session) = {
    TableQuery[ProfileCreditPointsRepo].where(_.userId === userId).firstOption
  }

  def findUserCreditDetails(userId: String)(implicit session: Session) = {
    TableQuery[CreditPointsDetailRepo].where(_.userId === userId).list()
  }
}
