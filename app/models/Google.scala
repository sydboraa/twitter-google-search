package models

import java.net.{ConnectException, URLEncoder}
import com.amazonaws.services.sqs.model.Message
import play.api.{Logger, Play}
import play.api.libs.ws.{WSResponse, WS, WSRequestHolder}
import utils.SQS
import scala.concurrent._
import play.api.Play.current
import scala.concurrent.ExecutionContext.Implicits.global
/**
 * Created by SB on 23/03/15.
 */

case class GoogleHelper(responseBody : String, fileUuid : String)
case class GoogleError(errorMessage: String)
case class GoogleRequest(uuidSqsMessage : Message, searchKey : String)

object Google {

  val getGoogleApiKey = Play.application.configuration.getString("google.apiKey").getOrElse("google.apiKey")
  val getSearchEngineKey = Play.application.configuration.getString("google.customSearchKey").getOrElse("google.customSearchKey")
  val getGoogleSearchUrl = Play.application.configuration.getString("google.searchUrl").getOrElse("google.searchUrl")

  def getSearchUrl(searchKey: String) : String = {
    val searchUrl : String = getGoogleSearchUrl + "key=" + getGoogleApiKey + "&cx=" + getSearchEngineKey + "&num=3&alt=json&q=" + URLEncoder.encode(searchKey, "UTF-8")
    searchUrl
  }

  def getSearchResults(request : GoogleRequest) : Future[(Option[GoogleHelper],Option[GoogleError])] = {
    try {
      val holder: WSRequestHolder = WS.url(getSearchUrl(request.searchKey))
      println("Google search key: " + request.searchKey)

      val fileUuid: String = request.uuidSqsMessage.getBody
      try {
        val futureResponse: Future[WSResponse] = holder.get()

        futureResponse.map(response => {
          response.status match {
            case 200 => {
              Logger.info("getSearchResults - Success")
              SQS.deleteMessage(request.uuidSqsMessage)
              (Some(GoogleHelper(response.body, fileUuid)), None)
            }
            case 403 => {
              Logger.info("getSearchResults - Daily limit exceeded!")
              (None, Some(GoogleError("Limit exceeded!")))
            }
            case _ => {
              Logger.info("getSearchResults - Something is wrong!")
              (Some(GoogleHelper(response.body, fileUuid)), Some(GoogleError("Something is wrong!")))
            }
          }
        }).recover({
          case ex: TimeoutException =>
            Logger.info("getSearchResults - Timeout exception " + ex)
            (None, Some(GoogleError("getSearchResults - Timeout exception" + ex)))
          case ex: ConnectException =>
            Logger.info("getSearchResults - Connection exception " + ex)
            (None, Some(GoogleError("getSearchResults - Connection exception" + ex)))
          case ex: Throwable =>
            Logger.error("getSearchResults - Unhandled general exception", ex)
            (None, Some(GoogleError("getSearchResults - General exception occurred!" + ex)))
        })
      } catch {
        case holderGetEx : Exception => Future(None, Some(GoogleError("getSearchResults - Cannot reach google search results!" + holderGetEx)))
      }
    } catch {
      case internalEx: Exception => Future(None, Some(GoogleError("getSearchResults - Something is wrong! " + internalEx)))
    }
  }

}
