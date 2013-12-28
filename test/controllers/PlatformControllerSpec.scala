package controllers

import models._
import services._

import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._

import play.api.libs.json.{Json, JsSuccess, JsError}
import play.api.test._
import play.api.test.Helpers._
import reactivemongo.bson.BSONObjectID

import scala.concurrent.Await
import scala.concurrent.duration._
import MongoDBTestUtils.withMongoDb
import java.util.Date

@RunWith(classOf[JUnitRunner])
object PlatformControllerSpec extends Specification {

  "the platform controller" should {

    "save a platform only once" in withMongoDb { implicit app =>
      val pr = FakeRequest().withBody(Json.obj("name" -> "HSBC",  "currency" -> "HKD"))
      val prlc = FakeRequest().withBody(Json.obj("name" -> "hsbc",  "currency" -> "CYN"))
      
      status(PlatformController.savePlatform(pr)) must_== CREATED
      status(PlatformController.savePlatform(pr)) must_== BAD_REQUEST
      status(PlatformController.savePlatform(prlc)) must_== BAD_REQUEST
      
      val platforms = Await.result(PlatformDao.findAll(), Duration(5, SECONDS))
      platforms must haveSize(1)

      val platform = platforms.head.as[Platform]
      platform.name must_== "HSBC"
      platform.currency must_== "HKD"
    }
    
    "save a platform using uppercase code" in withMongoDb { implicit app =>
      val pr = FakeRequest().withBody(Json.obj("name" -> "hsbc",  "currency" -> "HKD"))
      
      status(PlatformController.savePlatform(pr)) must_== CREATED
      
      val platforms = Await.result(PlatformDao.findAll(), Duration(5, SECONDS))
      platforms must haveSize(1)

      val platform = platforms.head.as[Platform]
      platform.name must_== "HSBC"
      platform.currency must_== "HKD"
    }
    
    "find a platform with different case" in withMongoDb { implicit app =>
      val pr = FakeRequest().withBody(Json.obj("name" -> "HSBC",  "currency" -> "HKD"))
      
      status(PlatformController.savePlatform(pr)) must_== CREATED
      
      val exists = Await.result(PlatformDao.exists("name", "hSBc"), Duration(5, SECONDS))
      exists must_== true

      val platforms = Await.result(PlatformDao.find("name", "hsbc"), Duration(5, SECONDS))
      platforms must haveSize(1)

      val platform = platforms.head.as[Platform]
      platform.name must_== "HSBC"
      platform.currency must_== "HKD"
    }
    
    "delete a platform without transactions" in withMongoDb { implicit app =>
       val pr = FakeRequest().withBody(Json.obj("name" -> "HSBC",  "currency" -> "HKD"))
       status(PlatformController.savePlatform(pr)) must_== CREATED 
       
       val platformsBefore = Await.result(PlatformDao.findAll(), Duration(5, SECONDS))
       platformsBefore must haveSize(1)
       
       status(PlatformController.deletePlatform(FakeRequest().withBody(
        Json.toJson(platformsBefore.head))
       )) must_== OK
      
       val platformsAfter = Await.result(PlatformDao.findAll(), Duration(5, SECONDS))
       platformsAfter must haveSize(0)
    }

    "not delete a platform with a transaction" in withMongoDb { implicit app =>
       val productreq = FakeRequest().withBody(Json.obj("code" -> "GOOG",  "description" -> "Description"))
       val platformreq = FakeRequest().withBody(Json.obj("name" -> "HSBC",  "currency" -> "HKD"))
       status(ProductController.saveProduct(productreq)) must_== CREATED 
       status(PlatformController.savePlatform(platformreq)) must_== CREATED 
       
       Await.result(TransactionDao.save(Transaction
         (BSONObjectID.generate, new Date(), -1, "HSBC", "GOOG", 0, 1000, 700, Some(Buy), 0)), Duration(5, SECONDS))
       
       val platformsBeforeDelete = Await.result(PlatformDao.findAll(), Duration(5, SECONDS))
       platformsBeforeDelete must haveSize(1)
       
       status(PlatformController.deletePlatform(FakeRequest().withBody(
        Json.toJson(platformsBeforeDelete.head))
       )) must_== BAD_REQUEST
      
       val platformAfterDelete = Await.result(PlatformDao.findAll(), Duration(5, SECONDS))
       platformAfterDelete must haveSize(1)
    }
    
  }
 
}