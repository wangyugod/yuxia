package controllers

import play.api.mvc._
import play.api.libs.Files.TemporaryFile
import play.api.Logger

/**
 * Created with IntelliJ IDEA.
 * User: simonwang
 * Date: 8/14/13
 * Time: 4:35 PM
 * To change this template use File | Settings | File Templates.
 */
trait Secured {


  def loginKey = "user_login"

  /**
   * Retrieve the connected user email.
   */
  protected def username(request: RequestHeader) = request.session.get(loginKey)

  /**
   * Redirect to login if the user in not authorized.
   */
  protected def onUnauthorized(request: RequestHeader) = {
    val forward = request.getQueryString("forward") match {
      case Some(url) => url
      case _ => request.uri
    }
    println("queryString is " + request.uri)
    Results.Redirect(routes.ProfileController.login + "?forward=" + forward )
  }

  // --

  /**
   * Action for authenticated users.
   */
  def isAuthenticated(f: Request[AnyContent] => Result) = Security.Authenticated(username, onUnauthorized) {
    user => Action(f)
  }


  def isAuthenticatedWithMultipartParser(parser: BodyParser[MultipartFormData[TemporaryFile]])(f: Request[MultipartFormData[TemporaryFile]] => Result) = Security.Authenticated(username, onUnauthorized) {
    user => Action(parser)(f)
  }
}

trait MerchSecured extends Secured {
  override def loginKey = "merch_login"

  override def onUnauthorized(request: RequestHeader) = Results.Redirect(routes.Merchandise.login)
}


trait InternalMgtSecured extends Secured {
  override def loginKey = "internal_login"

  override def onUnauthorized(request: RequestHeader) = Results.Redirect(routes.InternalManagement.login)
}
