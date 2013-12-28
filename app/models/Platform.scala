package models

import play.api.libs.json.{Json, JsValue}
import reactivemongo.bson.BSONObjectID
import play.modules.reactivemongo.json.BSONFormats._

// The name is unique
// The currency is the ISO 4217 code, see java.util.Currency
case class Platform(_id: BSONObjectID, name: String, currency: String)

object Platform { implicit val platformFormat = Json.format[Platform] }