package controllers

import models._
import org.specs2.mutable.Specification
import play.api.libs.iteratee.Iteratee
import play.api.libs.json.Json
import reactivemongo.bson.BSONObjectID
import reactivemongo.bson.BSONDateTime
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import services.{EventDao, TransactionDao}
import MongoDBTestUtils.withMongoDb

object EventsSpec extends Specification {

  "Events" should {

    "publish a new event when a message is saved" in withMongoDb { implicit app =>
      val futureEvent = EventDao.stream |>>> Iteratee.head
      val now = new BSONDateTime(System.currentTimeMillis())
      val transaction = Transaction(BSONObjectID.generate, now, -1, "XXXX", 0, 100, 10.5, Side.parse("Buy"), 0)
      TransactionDao.save(transaction)

      Await.result(futureEvent, Duration.Inf) must beSome.like {
        case event =>
          event.name must_== "transaction"
          Json.fromJson[Transaction](event.data).asOpt must beSome(transaction)
      }
    }
  }
}
