package helper

import slick.session.Database
import play.api.db.DB
import play.api.Play.current

/**
 * Created with IntelliJ IDEA.
 * User: simonwang
 * Date: 6/13/13
 * Time: 11:19 PM
 * To change this template use File | Settings | File Templates.
 */
object DBHelper {
  lazy val database = Database.forDataSource(DB.getDataSource())
}
