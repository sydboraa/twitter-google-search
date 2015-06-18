package utils

/**
 * Created by SB on 23/03/15.
 */

import com.amazonaws.{AmazonClientException, AmazonServiceException}
import com.amazonaws.services.sqs.{AmazonSQSAsyncClient}
import com.amazonaws.services.sqs.model._
import play.api.{Logger}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._

case class SQSError(errorMsg : String)

object SQS extends AmazonCredentials {

  val sqs : AmazonSQSAsyncClient = new AmazonSQSAsyncClient(awsCredentials)

  def sendMessage(jobMessage : String) : Future[Option[SendMessageResult]] = {
    try{
      val sendJobRequest : SendMessageRequest= new SendMessageRequest(awsQueueUrl, jobMessage)

      val futureSendMessagesResult : Future[SendMessageResult] = getScalaFuture[SendMessageRequest, SendMessageResult, java.util.concurrent.Future[SendMessageResult]](
        asyncHandler => {
          sqs.sendMessageAsync(sendJobRequest, asyncHandler)
        })

      futureSendMessagesResult.map {
        sendMessageResult => Some(sendMessageResult)
      }.recover({
        case ex: AmazonServiceException =>
          Logger.info("getToken - Timeout exception " + ex)
          None
        case ex: AmazonClientException =>
          Logger.info("getToken - Connection exception " + ex)
          None
      })
    } catch {
      case ex : Exception => {
        Logger.error("sendMessage - Something is wrong! " + ex.toString)
        Future(None)
      }
    }
  }

  def receiveMessage : Future[Option[ReceiveMessageResult]] = {
    //(Option[List[Message]],Option[SQSError]) = {
    try{
      val receivedMessage : ReceiveMessageRequest = new ReceiveMessageRequest(awsQueueUrl)
      receivedMessage.setMaxNumberOfMessages(10)
      //      val result : ReceiveMessageResult = sqs.receiveMessage(receivedMessage)
      //      (Some(result.getMessages.asScala.toList),None)

      val futureGetMessagesResult: Future[ReceiveMessageResult] = getScalaFuture[ReceiveMessageRequest, ReceiveMessageResult, java.util.concurrent.Future[ReceiveMessageResult]](
        asyncHandler => {
          sqs.receiveMessageAsync(receivedMessage, asyncHandler)
        })
      futureGetMessagesResult.map {
        getMessageResult => Some(getMessageResult)
      }.recover({
        case ex: AmazonServiceException =>
          Logger.info("getToken - Timeout exception " + ex)
          None
        case ex: AmazonClientException =>
          Logger.info("getToken - Connection exception " + ex)
          None
      })
    }
    catch {
      case ex : Exception => {
        Logger.error("receiveMessage - Something is wrong in SQS" + ex.toString)
        Future(None)
      }
    }

  }

  def deleteMessage(msg : Message): Unit = {
    try{
      val deletedMessage = new DeleteMessageRequest(awsQueueUrl, msg.getReceiptHandle())
      sqs.deleteMessage(deletedMessage)
    }
    catch {
      case ex : Exception => {
        Logger.error("deleteMessage - Something is wrong! " + ex.toString)
      }
    }

  }
}
