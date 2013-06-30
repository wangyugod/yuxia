package helper

import java.sql.Date

/**
 * Created with IntelliJ IDEA.
 * User: simonwang
 * Date: 6/14/13
 * Time: 4:46 PM
 * To change this template use File | Settings | File Templates.
 */
object AppHelper {

  def convertBirthdayFromText(date: Option[String]) = date match {
    case (Some(x)) => {
      val DATE_FORMAT = "yyyy-MM-dd"
      val sdf = new java.text.SimpleDateFormat(DATE_FORMAT)
      val d = sdf.parse(x)
      println("date is " + d)
      Some(new Date(d.getTime))
    }
    case _ => None
  }

  def convertBirthdayToText(birthday: Option[Date]): Option[String] = birthday match {
    case Some(date) => {
      val DATE_FORMAT = "yyyy-MM-dd"
      val sdf = new java.text.SimpleDateFormat(DATE_FORMAT);
      Some(sdf.format(date))
    }
    case None => None
  }
}