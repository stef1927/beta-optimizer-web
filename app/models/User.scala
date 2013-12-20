package models

import play.api.libs.json.Json
import reactivemongo.bson.BSONObjectID
import play.modules.reactivemongo.json.BSONFormats._
  
case class User(_id: BSONObjectID, name: String)
object User { implicit val userFormat = Json.format[User] }

