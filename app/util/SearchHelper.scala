package util

import play.api._
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.client.utils.URIBuilder
import play.api.libs.json.{Json, JsObject}
import scala.io.Source
import play.api.mvc.{AnyContent, Request}
import vo.SearchResult

/**
 * Created with IntelliJ IDEA.
 * User: simonwang
 * Date: 8/22/13
 * Time: 7:58 AM
 * To change this template use File | Settings | File Templates.
 */
object SearchHelper {

  private val log = Logger(this.getClass)
  lazy val searchEngineUrl = Play.current.configuration.getString("solr.url").get

  def doQuery(parameters: Map[String, String]) = {
    if(log.isDebugEnabled)
      log.debug("query started with parameters " + parameters)

    val client = new DefaultHttpClient()
    try{
      val httpGet = new HttpGet(searchEngineUrl)
      val builder = new URIBuilder(httpGet.getURI)

      for(key <- parameters.keySet){
        builder.addParameter(key, parameters.get(key).get)
      }
      val uri = builder.build()
      httpGet.setURI(uri)
      if(log.isDebugEnabled)
        log.debug("prepare to connect search server with uri " + uri)
      val response = client.execute(httpGet)
      val lines = Source.fromInputStream(response.getEntity.getContent()).getLines().mkString("\n")
      if(log.isDebugEnabled)
        log.debug("search response:\n " + lines)
      val result = Json.parse(lines).as[JsObject]
      result
    } finally {
      client.getConnectionManager().shutdown();
    }
  }

  def query(fieldName: String, fieldValue: String, request: Request[AnyContent]) = {
    val pageParameter = request.queryString.get("page")
    val pageNum: Int = pageParameter match {
      case Some(x) => x.head.toInt - 1
      case _ => 0
    }
    if (log.isDebugEnabled)
      log.debug("current page is " + pageNum)
    val rows = Play.current.configuration.getInt("pagination.quantity").get
    val start = pageNum * rows
    val params = Map("q" -> (fieldName + ":" + fieldValue), "wt" -> "json", "indent" -> "true", "rows" -> rows.toString, "start" -> start.toString)
    val result = doQuery(params)
    val searchResult = SearchResult(result)
    if (log.isDebugEnabled)
      log.debug("search result is " + searchResult)
    searchResult
  }
}
