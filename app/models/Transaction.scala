package models

import play.api.libs.json.Json
import play.api.libs.json._
import play.api.libs.functional.syntax._
import reactivemongo.bson.BSONObjectID
import reactivemongo.bson.BSONDateTime
import play.modules.reactivemongo.json.BSONFormats._
import scala.math.BigDecimal

sealed trait Side {
  def descr: String
  
  override def toString: String = descr
}

object Side {
  def parse(descr: String): Option[Side] = descr match { 
    case Buy.descr => Some(Buy)
    case Sell.descr => Some(Sell)
    case _ => None
  }
   
  implicit def reads: Reads[Side] = new Reads[Side] {
    def reads(json: JsValue): JsResult[Side] = json match {
      case JsString(v) => parse(v) match {
        case Some(a) => JsSuccess(a)
        case _ => JsError(s"String value ($v) is not a valid side ")
      }
      case _ => JsError("String value expected")
    }
  }

  implicit def writes[Side]: Writes[Side] = new Writes[Side] {
    def writes(side: Side): JsValue = JsString(side.toString)
  }
  
  implicit def sideFormat = Format(reads, writes)
}

case object Buy extends Side { val descr = "Buy" } 
case object Sell extends Side { val descr = "Sell" }

case class Transaction(_id: BSONObjectID, timestamp: BSONDateTime, userid: Int, product: String,
    exchange: Int, quantity: Int, price: BigDecimal, side: Option[Side], cost: BigDecimal)

object Transaction { implicit val transactionFormat = Json.format[Transaction] }