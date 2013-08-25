package helper

import play.api._
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.client.utils.URIBuilder
import play.api.libs.json.{Json, JsObject}
import scala.io.Source

/**
 * Created with IntelliJ IDEA.
 * User: simonwang
 * Date: 8/22/13
 * Time: 7:58 AM
 * To change this template use File | Settings | File Templates.
 */
object SearchHelper {

  lazy val searchEngineUrl = Play.current.configuration.getString("solr.url").get

  def query(parameters: Map[String, String]) = {
    val client = new DefaultHttpClient()
    try{
      val httpGet = new HttpGet(searchEngineUrl)
      val builder = new URIBuilder(httpGet.getURI)

      for(key <- parameters.keySet){
        builder.addParameter(key, parameters.get(key).get)
      }

      val uri = builder.build()
      httpGet.setURI(uri)
      val response = client.execute(httpGet)
      val lines = Source.fromInputStream(response.getEntity.getContent()).getLines().mkString("\n")
      val result = Json.parse(lines)
      result
    } finally {
      client.getConnectionManager().shutdown();
    }
  }

}
