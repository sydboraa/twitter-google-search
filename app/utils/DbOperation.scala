package utils

import anorm._
import com.mysql.jdbc.exceptions.MySQLTimeoutException
import play.api.Logger
import play.api.db.DB
import play.api.Play.current
import anorm.SqlParser._
/**
 * Created by SB on 23/03/15.
 */
object DbOperation {

  def uuid : String = java.util.UUID.randomUUID.toString

  def insertUser(name : String) : Option[String] = {
    val dbUuid : String = uuid
    try{
      if(dbUuid != null){
        val id: Option[Long] = DB.withConnection { implicit c =>
          SQL("insert into user(UUID, name, status) values ({UUID}, {name}, {status})")
            .on('UUID -> dbUuid, 'name -> name, 'status -> "Pending").executeInsert()
        }
        val insertedId : Long = id.getOrElse(0)
        if(insertedId != 0){
          Logger.info("User inserted db: " + insertedId)
          Some(dbUuid)
        }
        else{
          Logger.error("User cannot insert to db: " + insertedId)
          None
        }
      }
      else{
        Logger.error("insertUser - UUID cannot create!")
        None
      }
    }
    catch {
      case ex : Exception => {
        Logger.error("insertUser - " + ex.toString)
        None
      }
    }
  }

  def getUserName(UUID: String) : Option[String] = DB.withConnection { implicit c =>
    try{
      val row : Option[String] = SQL("select * from user where UUID = {UUID}")
        .on('UUID -> UUID)
        .as((str("name")).singleOpt)
      row
    }
    catch {
      case ex : Exception => {
        Logger.error("getUserName - " + ex.toString)
        None
      }
    }
  }

  def getUUIDs : Option[List[String]] = DB.withConnection { implicit c =>
    try{
      val row : List[String] = SQL("select * from user where status = 'Pending' ").as(str("UUID").*)
      println("Status Pending: " + row)
      Some(row)
    }
    catch {
      case timeoutEx : MySQLTimeoutException => {
        Logger.error("Timeout exception occurred! " + timeoutEx.getMessage)
        None
      }
      case ex : Exception => {
        Logger.error("getUUIDs - " + ex.toString)
        None
      }
    }
  }

  def updateStatus(UUID : String) : Boolean = {
    try{
      val result : Int = DB.withConnection { implicit c =>
        SQL("update user set status = 'OK' where UUID = {UUID}").on('UUID -> UUID).executeUpdate()
      }
      if(result > 0){
        Logger.info("Update successful : " + UUID)
        true
      }
      else {
        Logger.error("User cannot update. UUID " + UUID)
        false
      }
    }
    catch {
      case ex : Exception => {
        Logger.error("Something is wrong! " + ex.toString)
        false
      }
    }
  }

}
