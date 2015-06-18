package actors

/**
 * Created by SB on 23/03/15.
 */
object ActorsProtocol {
  case class InvokeMessage[T](sqsMessage : T)
  case class Job[T, B](content: T, f: (T => B))
  case class JobDone[B](result: B)
  case class SendJob[T](result : T)
  case class ErrorMessage(message: String, error: Option[Throwable])
}
