package controllers

import play.api.mvc._
import play.api.libs.Files.TemporaryFile

/**
 * Created with IntelliJ IDEA.
 * User: simonwang
 * Date: 8/14/13
 * Time: 4:35 PM
 * To change this template use File | Settings | File Templates.
 */
trait Secured {
  /**
   * Retrieve the connected user email.
   */
  protected def username(request: RequestHeader) = request.session.get("login")

  /**
   * Redirect to login if the user in not authorized.
   */
  protected def onUnauthorized(request: RequestHeader) = Results.Redirect(routes.ProfileController.login)

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
  override def username(request: RequestHeader) = request.session.get("merch_login")

  override def onUnauthorized(request: RequestHeader) = Results.Redirect(routes.Merchandise.login)
}
