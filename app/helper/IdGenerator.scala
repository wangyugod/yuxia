package helper

import java.util.UUID

/**
 * Created with IntelliJ IDEA.
 * User: simonwang
 * Date: 5/27/13
 * Time: 6:40 PM
 * To change this template use File | Settings | File Templates.
 */
object IdGenerator {

  val PROFILE_PREFIX = "p"
  val ACCT_PREFIX = "a"
  val TAG_PREFIX = "t"

  def generateProfileId() = {
    PROFILE_PREFIX + UUID.randomUUID()
  }

  def generateAccountingId() = {
    ACCT_PREFIX + UUID.randomUUID()
  }

  def generateTagId() = {
    TAG_PREFIX + UUID.randomUUID()
  }

}
