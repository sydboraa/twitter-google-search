package utils

import com.amazonaws.auth.{BasicAWSCredentials, AWSCredentials}
import com.amazonaws.handlers.AsyncHandler
import play.api.Play.current
import play.api.Play

import scala.concurrent.{Promise, ExecutionContext}
import scala.util.control.NonFatal

/**
 * Created by SB on 24/03/15.
 */
class AmazonCredentials {

  val awsAccessKey : String = Play.application.configuration.getString("aws.accessKey").getOrElse("aws.accessKey")
  val awsSecretKey : String = Play.application.configuration.getString("aws.secretKey").getOrElse("aws.secretKey")
  val awsCredentials :  AWSCredentials = new BasicAWSCredentials(awsAccessKey,awsSecretKey)
  val awsQueueUrl : String = Play.application.configuration.getString("aws.sqs.url").getOrElse("aws.sqs.url")

  val awsBucket : String = Play.application.configuration.getString("aws.s3.bucket").getOrElse("aws.s3.bucket")


    def getScalaFuture[T <: com.amazonaws.AmazonWebServiceRequest, R, U](f: AsyncHandler[T, R] => U)(implicit executionContext: ExecutionContext): scala.concurrent.Future[R] = {
    val p = Promise[R]()
    try {
      f(new AsyncHandler[T, R] {
        def onError(e: Exception): Unit = p failure e

        def onSuccess(request: T, response: R): Unit = p success response
      })
    } catch {
      case NonFatal(t) => p failure t
    }
    p.future
  }

}
