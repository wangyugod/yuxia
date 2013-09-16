package util

import play.api.i18n.Messages
import play.api.Logger

/**
 * Created with IntelliJ IDEA.
 * User: simonwang
 * Date: 9/16/13
 * Time: 4:26 PM
 * To change this template use File | Settings | File Templates.
 */
object AddressInfo {
  private val log = Logger(this.getClass())

  val PROVINCE_PREFIX = "prov"
  val CITY_PREFIX = "city"
  val DISTRICT_PREFIX = "dist"
  val TEXT_SEPARATOR = "."

  /*province*/
  val provinceCityMap = Map("sc" -> List("cd"))
  val cityDistrictMap = Map("cd" -> List("gx", "wh", "qy", "ch", "jn", "jj", "lq", "hy"))

  val defaultProvince = "sc"
  val defaultCity = "cd"


  def cityName(code: String) = {
    Messages(CITY_PREFIX + TEXT_SEPARATOR + code)
  }

  def distName(code: String) = {
    Messages(DISTRICT_PREFIX + TEXT_SEPARATOR + code)
  }

  def provinceName(code: String) = {
    Messages(PROVINCE_PREFIX + TEXT_SEPARATOR + code)
  }



}
