package controllers

import java.net.{PasswordAuthentication, Authenticator}

import com.amazonaws.services.sqs.model.{SendMessageResult}
import org.apache.http.HttpHost
import org.apache.http.auth.{UsernamePasswordCredentials, AuthScope}
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.{CloseableHttpResponse, HttpGet, HttpPost}
import org.apache.http.impl.client.{BasicCredentialsProvider, HttpClients, CloseableHttpClient}
import org.apache.http.util.EntityUtils
import play.api.{Logger}
import play.api.data.Form
import play.api.mvc.{Action, Controller}
import utils.{ SQS, DbOperation}
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.data.Forms._

import scala.concurrent.Future

/**
 * Created by SB on 23/03/15.
 */
case class User(userName : String)

object CreateUser extends Controller {

  val form : Form[User] = Form(
    mapping(
      "userName" -> nonEmptyText
    )(User.apply)(User.unapply)
  )

  def create = Action { implicit request =>

    def values = form.bindFromRequest.data
    val userName = Some(values("userName")).getOrElse("")

    if(userName != ""){
      Logger.info("Now we have user's name: " + userName)

      val insertResult : (Option[String]) = DbOperation.insertUser(userName)

      insertResult match {
        case Some(uuid) => {

          val sqsResult : Future[Option[SendMessageResult]] = SQS.sendMessage(uuid)

          sqsResult onSuccess {
            case maybeSqsMsg => {
              Logger.info("Write to sqs successfully")
            }
            case None => {
              Logger.error("Message cannot save to sqs ")
            }
          }

          sqsResult onFailure  {
            case failResult => {
              Logger.error("Message cannot save to sqs " + failResult.getMessage)
            }
          }

        }
        case None => {
          Logger.error("User cannot insert to db")
        }
      }
    }
    else{
      Logger.warn("Enter a username")
    }
    Ok(views.html.index("Ok"))
  }


}
