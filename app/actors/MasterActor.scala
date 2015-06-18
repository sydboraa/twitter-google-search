package actors

import actors.ActorsProtocol._
import akka.actor.{ActorSystem, Props, Actor}
import akka.routing.RoundRobinPool
import com.amazonaws.services.sqs.model.{ReceiveMessageResult, Message}
import models.{Google, GoogleRequest, GoogleHelper, GoogleError}
import play.api.Logger
import play.api.Play.current
import play.api.libs.concurrent.Akka
import utils.{DbOperation, S3, SQSError, SQS}
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
/**
 * Created by SB on 23/03/15.
 */
class MasterActor(nrOfWorkers : Int) extends Actor {

  val gWorker = context.actorOf(Props[QueryExecutorActor[Job[GoogleRequest, (Option[GoogleHelper], Option[GoogleError])], JobDone[(Option[GoogleHelper], Option[GoogleError])]]].withRouter(RoundRobinPool(nrOfWorkers)), name = "gWorker")

  def receive = {

    case SendJob(resultMsg: Message) => {

      val userName : Option[String] = DbOperation.getUserName(resultMsg.getBody)

      Logger.info("Now searching : " + userName)

      userName match {
        case Some(maybeUserName) => {
          gWorker ! Job(new GoogleRequest(resultMsg,maybeUserName), Google.getSearchResults)
        }
        case None => {
          Logger.error("Cannot reach search key!")
        }
      }
    }

    case JobDone(googleResult : (Option[GoogleHelper], Option[GoogleError])) => {
      googleResult match {
        case (Some(googleHelper), None) => {
          S3.saveFile(googleHelper.fileUuid , googleHelper.responseBody)
        }
        case (Some(googleHelper),Some(googleError)) => {
          Logger.error("Something is wrong JobDone! " + googleError.errorMessage)
          S3.saveFile(googleHelper.fileUuid , googleHelper.responseBody)
        }
        case (None, Some(googleError)) => {
          Logger.error("Something is wrong JobDone! " + googleError.errorMessage)
        }
        case (None,None) => {
          Logger.error("Something is wrong. Unhandled exception occurred!")
        }
      }
    }
  }
}

object MasterActor {

  val system = ActorSystem("FrameworkSystem")
  val master = system.actorOf(Props(new MasterActor(3)),name = "master")

  def getSQSMessages = {
    val maybeMessage: Future[Option[ReceiveMessageResult]] = SQS.receiveMessage

    println("dfadsfkjaskldjfhkdshf")

    maybeMessage onSuccess {
      case messages => {
        for (result <- messages) {
          master ! SendJob(result)
        }
      }
      case _ => Logger.error("What going on?")
    }
    maybeMessage onFailure {
      case failResult => Logger.error("Something is wrong on failure - " + failResult)
    }
  }

  def start {
    Akka.system.scheduler.schedule(0 seconds, 10 seconds) {
      getSQSMessages
    }
  }

}
