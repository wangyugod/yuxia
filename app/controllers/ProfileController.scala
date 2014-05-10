package controllers

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.Play.current
import validation.Constraints
import views.html

import models._
import util._
import play.api.i18n.Messages
import play.api.Logger
import vo.AddressVo
import java.util.{Date, UUID}
import scala.slick.lifted.TableQuery
import org.apache.commons.lang3.RandomStringUtils

/**
 * Created with IntelliJ IDEA.
 * User: simonwang
 * Date: 6/13/13
 * Time: 11:24 PM
 * To change this template use File | Settings | File Templates.
 */

trait Users {

  val LOGIN_KEY = "user_login"
  val USER_NAME = "user_name"
  val USER_ID = "user_id"
  val CURR_ORDER_ID = "curr_order_id"
  val LAST_ORDER_ID = "last_order_id"

  implicit def toUser(implicit session: Session): Option[Profile] = {
    session.get(LOGIN_KEY) match {
      case Some(login) =>
        println(s"found login in session $login")
        Some(Profile(session.get(USER_ID).get, session.get(LOGIN_KEY).get, "", "", session.get(USER_NAME).get, None, None))
      case None =>
        println("No login in session found")
        None
    }
  }
}

object ProfileController extends Controller with Users with Secured {
  private val log = Logger(this.getClass)
  private val PWD_MAIL_CODE = "mailcode"


  val profileForm: Form[Profile] = Form(
    mapping(
      "id" -> optional(text),
      "login" -> email,
      "password" -> tuple(
        "main" -> text.verifying(Messages("error.password.minlength"), {
          _.length >= 6
        }),
        "confirm" -> text
      ).verifying(
          // Add an additional constraint: both passwords must match
          Messages("error.password.notmatch"), passwords => passwords._1 == passwords._2
        ),
      "name" -> nonEmptyText
    )
      ((id, login, passwords, name) => Profile(id.getOrElse(LocalIdGenerator.generateProfileId), login, passwords._1, RandomStringUtils.random(2), name, None, None))
      ((profile: Profile) => {
        Some((Some(profile.id), profile.login, (profile.password, ""), profile.name))
      }) verifying(Messages("error.login.alreadyexist"), profile => Profile.findUserByLogin(profile.login).isEmpty)
  )

  val profileUpdateForm: Form[Profile] = Form(
    mapping(
      "id" -> optional(text),
      "login" -> email,
      "name" -> nonEmptyText,
      "gender" -> optional(text),
      "birthday" -> optional(text.verifying(Constraints.pattern( """(19|20)\d\d[-](0[1-9]|1[012])[-](0[1-9]|[12][0-9]|3[01])""".r, "Date Constraint", Messages("error.myacct.birthday"))))
    )
      ((id, login, name, gender, birthday) => Profile(id.getOrElse(LocalIdGenerator.generateProfileId), login, "", "", name, gender, AppHelper.convertDateFromText(birthday)))
      ((profile: Profile) => {
        Some((Some(profile.id), profile.login, profile.name, profile.gender, AppHelper.convertDateToText(profile.birthDay)))
      }) //verifying("User with the same loign already exists", profile => ProfileService.findUserByLogin(profile.login).isEmpty)
  )

  val loginForm: Form[(String, String, Option[String])] = Form(
    tuple(
      "login" -> email,
      "password" -> nonEmptyText,
      "forwardURL" -> optional(text)
    )
  )

  val addressForm: Form[AddressVo] = Form(
    mapping(
      "id" -> optional(text),
      "province" -> nonEmptyText,
      "city" -> nonEmptyText,
      "district" -> nonEmptyText,
      "contactPhone" -> nonEmptyText,
      "addressLine" -> nonEmptyText,
      "contactPerson" -> nonEmptyText,
      "areaId" -> nonEmptyText,
      "isDefaultAddress" -> optional(boolean)
    )((id, province, city, district, contactPhone, addressLine, contactPerson, areaId, isDefaultAddress) => AddressVo(id.getOrElse(LocalIdGenerator.generateAddressId()), province, city, district, contactPhone, addressLine, contactPerson, Some(areaId), isDefaultAddress))
      ((addr: AddressVo) => Some(Some(addr.id), addr.province, addr.city, addr.district, addr.contactPhone, addr.addressLine, addr.contactPerson, addr.areaId.get, addr.isDefaultAddress))
  )

  def login = Action {
    implicit request =>
      Ok(html.login(loginForm)).withCookies(Cookie("DG", scala.util.Random.nextString(2)))
  }

  def authenticate = Action {
    implicit request =>
      loginForm.bindFromRequest.fold(
        formWithErrors => {
          BadRequest(html.login(formWithErrors))
        },
        user => {
          val foundUser = Profile.authenticateUser(user._1, user._2)
          if (foundUser.isDefined) {
            val order = Profile.findCurrentOrder(foundUser.get.id)
            val forwardUrl = user._3
            log.debug(s"forwardURL is $user._3")
            forwardUrl match {
              case Some(url) =>
                if (order.isDefined)
                  Redirect(url).withSession(loginKey -> foundUser.get.login, USER_NAME -> foundUser.get.name, USER_ID -> foundUser.get.id, CURR_ORDER_ID -> order.get.id)
                else
                  Redirect(url).withSession(loginKey -> foundUser.get.login, USER_NAME -> foundUser.get.name, USER_ID -> foundUser.get.id)

              case _ =>
                if (order.isDefined)
                  Redirect(routes.Application.index()).withSession(loginKey -> foundUser.get.login, USER_NAME -> foundUser.get.name, USER_ID -> foundUser.get.id, CURR_ORDER_ID -> order.get.id)
                else
                  Redirect(routes.Application.index()).withSession(loginKey -> foundUser.get.login, USER_NAME -> foundUser.get.name, USER_ID -> foundUser.get.id)

            }
          } else {
            BadRequest(html.login(loginForm.withError("login.failed", Messages("login.failed.msg"))))
          }
        }
      )
  }

  def signup = Action {
    implicit request =>
      Ok(html.signup(profileForm))
  }

  def create = Action {
    implicit request => profileForm.bindFromRequest.fold(
      formWithErrors => {
        if (log.isDebugEnabled)
          log.debug("Error form is " + formWithErrors)
        BadRequest(html.signup(formWithErrors))
      },
      profile => {
        Profile.createUser(profile)
        Redirect(routes.Application.index()).withSession(loginKey -> profile.login, USER_NAME -> profile.name, USER_ID -> profile.id)
      }
    )
  }

  def update = Action {
    implicit request => profileUpdateForm.bindFromRequest.fold(
      formWithErrors => {
        if (log.isDebugEnabled)
          log.debug("error form is \n" + formWithErrors.errors)
        BadRequest(html.myaccount.baseinfo(formWithErrors))
      },
      profile => {
        val oldProfile = Profile.findUserByLogin(profile.login).get
        val newProfile: Profile = Profile(profile.id, profile.login, oldProfile.password, oldProfile.pwdSalt, profile.name, profile.gender, profile.birthDay)
        Profile.updateUser(newProfile)
        Redirect(routes.ProfileController.myAccount())
      }
    )
  }

  def myAccount = isAuthenticated(
    implicit request => {
      log.debug("ORDER ID is " + request.session.get(CURR_ORDER_ID))
      val profile = Profile.findUserByLogin(request.session.get(loginKey).get)
      Ok(html.myaccount.baseinfo(profileUpdateForm.fill(profile.get)))
    }
  )

  def signout = Action {
    implicit request =>
      Redirect(routes.ProfileController.login()).withNewSession
  }

  def addressList = isAuthenticated {
    implicit request => {
      val userId = request.session.get(USER_ID).get
      val addresses = Address.userAddresses(userId)
      if (log.isDebugEnabled)
        log.debug("address list: " + addresses)
      Ok(html.myaccount.addressmgt(addressForm, addresses))
    }
  }

  def updateAddress = isAuthenticated {
    implicit request => {
      addressForm.bindFromRequest.fold(
        formWithErrors => {
          val userId = request.session.get(USER_ID).get
          val addresses = Address.userAddresses(userId)
          BadRequest(html.myaccount.addressmgt(formWithErrors, addresses))
        },
        address => {
          val userId = request.session.get(USER_ID).get
          if (log.isDebugEnabled)
            log.debug("is default address:" + address.isDefaultAddress)
          DBHelper.database.withTransaction {
            implicit session =>
              Address.saveOrUpdate(userId, address.address, address.isDefaultAddress.getOrElse(false))
          }
          Redirect(routes.ProfileController.addressList())
        }
      )
    }
  }

  def deleteAddress(id: String) = isAuthenticated {
    implicit request => {
      val userId = request.session.get(USER_ID).get
      DBHelper.database.withTransaction {
        implicit session =>
          Address.deleteUserAddress(userId, id)
      }
      Ok
    }
  }

  def orderHistory() = isAuthenticated {
    implicit request =>
      val userId = request.session.get(USER_ID).get
      val orderList =
        DBHelper.database.withSession {
          implicit session =>
            Profile.findUserOrders(userId)
        }
      Ok(html.myaccount.orderhistory(orderList))
  }

  def orderDetail(orderId: String) = isAuthenticated {
    implicit request =>
      val order = DBHelper.database.withSession {
        implicit session =>
          Order.findOrderById(orderId).get
      }
      Ok(html.myaccount.orderdetail(order))
  }

  def passwordManagement() = isAuthenticated {
    implicit request =>
      Ok(html.myaccount.passwordmgt())
  }

  def sendMailVerificationCode() = isAuthenticated {
    import com.typesafe.plugin._
    implicit request =>
      val mailCode = request.session.get(USER_ID).get + new Date().getTime()
      val encodedResult = Encoder.encodeSHA(mailCode)
      if (log.isDebugEnabled)
        log.debug(s"encoded $mailCode as $encodedResult")
      val userLogin = request.session.get(LOGIN_KEY).get
      val mail = use[MailerPlugin].email
      mail.setSubject(Messages("pwdmgt.mail.title"))
      mail.addFrom("wangyu4j@gmail.com")
      mail.addRecipient(userLogin)
      mail.send(Messages("pwdmgt.mail.code", encodedResult))
      Ok(encodedResult).withSession(request.session + (PWD_MAIL_CODE -> encodedResult))
  }

  val mailCodeForm = Form(
    single(
      "code" -> text
    )
  )

  def verifyPwdChangeMailCode = isAuthenticated {
    implicit request =>
      mailCodeForm.bindFromRequest.fold(
        formWithErrors => {
          BadRequest(html.myaccount.passwordmgt())
        },
        code => {
          request.session.get(PWD_MAIL_CODE) match {
            case Some(mailCode) if mailCode == code =>
              Redirect(routes.ProfileController.changePassword()).withSession(request.session - PWD_MAIL_CODE)
            case _ =>
              Redirect(routes.ProfileController.passwordManagement()).flashing("verify.result" -> Messages("pwdmgt.code.verify.fail"))
          }
        }
      )
  }

  def changePassword = isAuthenticated {
    implicit request =>
      Ok(html.myaccount.passwordchange(passwordForm))
  }

  val passwordForm:Form[(String, String)] = Form(
    tuple("mainPassword" -> nonEmptyText(),
      "confirmPassword" -> nonEmptyText()
    ).verifying(Messages("error.password.notmatch"), passwords => passwords._1 == passwords._2)
  )

  def updatePassword = isAuthenticated {
    implicit request =>
      passwordForm.bindFromRequest.fold(
        formWithErrors => {
          BadRequest(html.myaccount.passwordchange(formWithErrors))
        },
        form => {
          DBHelper.database.withTransaction{
            implicit session =>
              val userId = request.session.get(USER_ID).get
              Profile.updateProfilePassword(userId, form._1)
          }
          Redirect(routes.ProfileController.updatePasswordSucceed())
        }
      )
  }

  def userCredits = isAuthenticated{
    implicit request =>
      val userId = request.session.get(USER_ID).get
      val creditDetails = DBHelper.database.withSession{
        implicit session =>
          ProfileCreditPoints.findUserCreditDetails(userId)
      }
      if(log.isDebugEnabled)
        log.debug(s"credit detail list for user $userId is $creditDetails")
      Ok(html.myaccount.usercredits(creditDetails))
  }

  def updatePasswordSucceed = isAuthenticated{
    implicit request =>
      Ok(html.myaccount.updatepwdsuccess())
  }
}
