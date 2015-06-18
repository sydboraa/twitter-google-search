package actors

import actors.ActorsProtocol.{Job, JobDone}
import akka.actor.Actor
import play.api.Logger
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * Created by SB on 23/03/15.
 */
class QueryExecutorActor[T,B] extends Actor {
  def receive = {
    case Job(content: T, f : (T => Future[B])) => {
      println("Id: " + self.path.name)
      val mainSender = sender

      try{
        val result = f.apply(content)

        result onSuccess  {
          case result => {
            mainSender ! JobDone(result)
          }
        }
        result onFailure {
          case failResult => {
            mainSender ! akka.actor.Status.Failure(failResult)
          }
        }
      }catch{
        case ex : Exception => {
          Logger.error("Logger from QueryExecutorActor!" + ex.toString)
          mainSender ! akka.actor.Status.Failure(ex)
        }
      }
    }
    case _ => println("Unknown state from QueryExecutorActor!")

  }
}
