package controllers

import models._
import org.specs2.mutable.Specification
import play.api.libs.iteratee.Iteratee
import play.api.libs.json.Json
import reactivemongo.bson.BSONObjectID
import reactivemongo.bson.BSONDateTime
import scala.concurrent._
import scala.concurrent.duration._
import services._
import MongoDBTestUtils.withMongoDb
import org.specs2.runner._
import org.junit.runner._
import java.util.Date

@RunWith(classOf[JUnitRunner])
object EventsSpec extends Specification {

  "Events" should {
	 
    "be published as expected" in withMongoDb { implicit app =>
      var events = EventDao.stream |>>> Iteratee.head
      
      val platform = Platform(BSONObjectID.generate, "HSBC", "CYN")
      PlatformDao.save(platform)
      
      Await.result(events, Duration(5, SECONDS)) must beSome.like {
        case event =>
          event.name must_== "platformCreated"
          Json.fromJson[Platform](event.data).asOpt must beSome(platform)
      }
      
      events = EventDao.stream |>>> Iteratee.head
      
      val now = new Date()
      val product = Product(BSONObjectID.generate, "GOOG", "GOOG Description", 700, now)
      ProductDao.save(product)
      
      Await.result(events, Duration(5, SECONDS)) must beSome.like {
        case event =>
          event.name must_== "productCreated"
          Json.fromJson[Product](event.data).asOpt must beSome(product)
      }
      
      events = EventDao.stream |>>> Iteratee.head
      val transaction = Transaction(BSONObjectID.generate, now, -1, "HSBC", "GOOG", 0, 100, 10.5, Side.parse("Buy"), 0)
      TransactionDao.save(transaction)
      
      Await.result(events, Duration(5, SECONDS)) must beSome.like {
        case event =>
          event.name must_== "transactionCreated"
          Json.fromJson[Transaction](event.data).asOpt must beSome(transaction)
      }
      
      events = EventDao.stream |>>> Iteratee.head
      TransactionDao.delete(transaction)
      
      Await.result(events, Duration(5, SECONDS)) must beSome.like {
        case event =>
          event.name must_== "transactionDeleted"
          Json.fromJson[Transaction](event.data).asOpt must beSome(transaction)
      }
      
      events = EventDao.stream |>>> Iteratee.head
      ProductDao.delete(product)
      
      Await.result(events, Duration(5, SECONDS)) must beSome.like {
        case event =>
          event.name must_== "productDeleted"
          Json.fromJson[Product](event.data).asOpt must beSome(product)
      }
      
      events = EventDao.stream |>>> Iteratee.head
      PlatformDao.delete(platform)
      
      Await.result(events, Duration(5, SECONDS)) must beSome.like {
        case event =>
          event.name must_== "platformDeleted"
          Json.fromJson[Platform](event.data).asOpt must beSome(platform)
      }
    }
  }
}
