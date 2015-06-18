package utils

import java.io.{IOException, FileWriter, BufferedWriter, File}
import com.amazonaws.services.s3.model._
import com.amazonaws.{AmazonServiceException, AmazonClientException}
import com.amazonaws.services.s3.AmazonS3Client
import play.api.Logger

import scala.util.Try

/**
 * Created by SB on 24/03/15.
 */
case class S3Error(errorMsg : String)

object S3 extends AmazonCredentials {

  val s3 : AmazonS3Client = new AmazonS3Client(awsCredentials)

  def saveFile(uuid : String, fileText : String): (Option[PutObjectResult],Option[S3Error]) ={
    try{
      val file : Option[File] = createFile(uuid, fileText)
      file match {
        case Some(file) => {
          (Some(s3.putObject(awsBucket, uuid, file)),None)
        }
        case None => (None,Some(S3Error("Something is wrong to create file!")))
      }
    }
    catch {
      case clientEx : AmazonClientException => {
        Logger.error("saveFile - Something is wrong!" + clientEx.getMessage)
        (None,Some(S3Error(clientEx.getMessage)))
      }
      case serviceEx : AmazonServiceException => {
        Logger.error("saveFile - Something is wrong!" + serviceEx.getErrorMessage)
        (None,Some(S3Error(serviceEx.getErrorMessage)))
      }
      case ex : Exception => {
        Logger.error("saveFile - Something is wrong!" + ex.toString)
        (None,Some(S3Error(ex.toString)))
      }
    }
  }

  def createFile(uuid : String, fileText : String): Option[File] = {
    try{
      val fileName : String = "s3files/" + uuid + ".json"
      val file : File = new File(fileName)
      val output: BufferedWriter = new BufferedWriter(new FileWriter(file))
      output.write(fileText)
      Try(output.close())
      Some(file)
    }
    catch {
      case ioEx : IOException => {
        Logger.error("saveFile - Something is wrong!" + ioEx.toString)
        None
      }
      case ex : Exception => {
        Logger.error("saveFile - Something is wrong!" + ex.toString)
        None
      }
    }

  }

  def getFile(fileName : String): (Option[String],Option[S3Error]) = {
    try{
      println("Getting file : " + fileName)
      val file : S3Object = s3.getObject(awsBucket, fileName)
      (Some(file.getKey),None)
    }
    catch {
      case clientEx : AmazonClientException => {
        Logger.info("getFile - Not yet saved!" + clientEx.getMessage)
        (None,Some(S3Error(clientEx.getMessage)))
      }
      case serviceEx : AmazonServiceException => {
        Logger.warn("getfile - Note yet saved!" + serviceEx.getErrorMessage)
        (None,Some(S3Error(serviceEx.getErrorMessage)))
      }
      case ex : Exception => {
        Logger.error("getFile - Something is wrong!" + ex.toString)
        (None,Some(S3Error(ex.toString)))
      }
    }

  }
  //
  //  def getFiles {
  //    val objectListing = s3.listObjects(new ListObjectsRequest().withBucketName(awsBucket))
  //
  //    for (objectSummary : S3ObjectSummary <- objectListing.getObjectSummaries.toList) {
  //      println(objectSummary.getKey())
  //    }
  //
  //  }

}
