package models

import play.api.libs.json.{Json, JsValue}
import reactivemongo.bson.BSONObjectID
import play.api.libs.Comet.CometMessage
import play.api.libs.EventSource.{EventNameExtractor, EventIdExtractor}
import play.modules.reactivemongo.json.BSONFormats._


case class Event(_id: BSONObjectID, name: String, data: JsValue)

object Event {
  implicit val eventFormat = Json.format[Event]

  implicit val cometMessage = CometMessage[Event](e => Json.stringify(e.data))
  implicit val idExtractor = EventIdExtractor[Event](e => Some(e._id.stringify))
  implicit val nameExtractor = EventNameExtractor[Event](e => Some(e.name))
}