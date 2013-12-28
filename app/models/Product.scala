package models

import play.api.libs.json.Json
import play.api.libs.json._
import play.api.libs.functional.syntax._
import reactivemongo.bson.BSONObjectID
import reactivemongo.bson.BSONDateTime
import play.modules.reactivemongo.json.BSONFormats._
import scala.math.BigDecimal
import java.util.Date

/*
object ProductType extends Enumeration {
  type ProductType = Value
  val Stock, Bond = Value
}
import ProductType._
*/

// The code is unique
case class Product(_id: BSONObjectID, code: String, description: 
	String, marketValue: BigDecimal, valuationDate: Date)

object Product { implicit val stockFormat = Json.format[Product] }

