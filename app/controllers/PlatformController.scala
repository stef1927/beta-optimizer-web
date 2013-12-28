package controllers

import play.api.mvc._
import play.api.libs.json.Json
import play.api.libs.concurrent.Execution.Implicits._
import play.api.Logger
import models._
import services.PlatformDao
import play.api.libs.concurrent.Execution.Implicits._
import reactivemongo.bson.BSONObjectID
import reactivemongo.bson.BSONDateTime
import java.util.Date
import scala.util.{Success, Failure}

object PlatformController extends Controller {

  def getPlatforms() = Action { implicit req =>
    Async {
      for {
        platforms <- PlatformDao.findAll()
      } yield Ok(Json.toJson(platforms))
    }
  }
  
  case class PlatformForm(name: String, currency: String) {
    def toPlatform: Platform = {
      Platform(BSONObjectID.generate, name.toUpperCase, currency)
    }
  }

  implicit val platformFormFormat = Json.format[PlatformForm]
  
  def savePlatform = Action(parse.json) { req =>
    Logger.info("Save platform " + req.body)
    Json.fromJson[PlatformForm](req.body).fold(
      invalid => BadRequest("Bad platform data"),
      form => Async {
        PlatformDao.save(form.toPlatform).map(_ => Created) .recover { 
          case e => BadRequest(e.getMessage) 
        }
      }
    )
  }
  
  def deletePlatform = Action(parse.json) { req =>
    Logger.info("Delete platform " + req.body)
    Json.fromJson[Platform](req.body).fold(
      invalid => BadRequest("Bad platform data"),
      platform => Async {
        PlatformDao.delete(platform).map(_ => Ok).recover{ 
          case e => BadRequest(e.getMessage) 
        }
      }
    )
  }

}