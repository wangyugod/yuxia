package util

import org.apache.commons.codec.binary.Base64
import play.api.Logger

/**
 * Created by thinkpad-pc on 14-3-27.
 */
object Encoder {
  def encodeSHA(value: String) = {
    val md = java.security.MessageDigest.getInstance("SHA-1")
    md.digest(value.getBytes())
    Base64.encodeBase64String(md.digest(value.getBytes()))
  }

}
