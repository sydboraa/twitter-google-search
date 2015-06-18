package actors

import actors.ValidatorActor.UpdateStatus
import play.api.Logger
import play.api.libs.concurrent.Akka
import utils.{S3Error, S3, DbOperation}
import scala.concurrent.duration._
import play.api.Play.current
import scala.concurrent.ExecutionContext.Implicits.global

import akka.actor.{ActorSystem, Props, Actor}
/**
 * Created by SB on 25/03/15.
 */
class ValidatorActor(nrOfWorkers : Int) extends  Actor {

  def receive = {
    case UpdateStatus => {

      val pendingRecords : Option[List[String]] = DbOperation.getUUIDs

      pendingRecords match {
        case Some(maybePendingRecord) => {

          maybePendingRecord.map { uuidFileKey =>

            val maybeGetS3Files : (Option[String],Option[S3Error]) = S3.getFile(uuidFileKey)

            maybeGetS3Files match {
              case (Some(s3Files), None) => {
                DbOperation.updateStatus(uuidFileKey)
              }
              case (Some(s3Files),Some(s3Error)) => {
                Logger.error(s3Error.toString)
                DbOperation.updateStatus(uuidFileKey)
              }
              case (None,Some(s3Error)) => {
                Logger.info("Not yet saved files - " + s3Error.toString)
              }
              case(None, None) => {
                Logger.info("Not yet saved files.")
              }
            }
          }
        }
        case None => Logger.info("No pending records!")
      }
    }
  }
}

object ValidatorActor {

  case class UpdateStatus(pendingResult : List[String])

  val validatorSystem = ActorSystem("ValidatorSystem")
  val validator = validatorSystem.actorOf(Props(new ValidatorActor(3)),name = "validator")

  def getS3Files {
    validator ! UpdateStatus
  }

  def start {
    Akka.system.scheduler.schedule(30 seconds, 30 seconds) {
      getS3Files
    }
  }
}
