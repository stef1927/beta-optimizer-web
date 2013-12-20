package models

import play.api.libs.json.Json
import play.api.libs.json._
import play.api.libs.functional.syntax._
import reactivemongo.bson.BSONObjectID
import reactivemongo.bson.BSONDateTime
import play.modules.reactivemongo.json.BSONFormats._
import scala.math.BigDecimal
  
case class Exchange(_id: BSONObjectID, name: String, location: String, marketValue: BigDecimal, valuationDate: BSONDateTime)
object Exchange { implicit val exchangeFormat = Json.format[Exchange] }

object ProductType extends Enumeration {
  type ProductType = Value
  val Stock, Bond = Value
}
import ProductType._

case class Product(_id: BSONObjectID, code: String, description: String, exchanges: List[Exchange]) {
  val productType = ProductType.Stock
}

object Product { implicit val stockFormat = Json.format[Product] }

