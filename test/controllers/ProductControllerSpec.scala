package controllers

import models._
import services._

import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._

import play.api.libs.json.Json
import play.api.test._
import play.api.test.Helpers._
import reactivemongo.bson.BSONObjectID

import scala.concurrent.Await
import scala.concurrent.duration._
import MongoDBTestUtils.withMongoDb
import java.util.Date

@RunWith(classOf[JUnitRunner])
object ProductControllerSpec extends Specification {

  "the product controller" should {

    "save a product only once" in withMongoDb { implicit app =>
      val pr = FakeRequest().withBody(Json.obj("code" -> "GOOG",  "description" -> "Description"))
      val prlc = FakeRequest().withBody(Json.obj("code" -> "goog",  "description" -> "Description"))
      
      status(ProductController.saveProduct(pr)) must_== CREATED
      status(ProductController.saveProduct(pr)) must_== BAD_REQUEST
      status(ProductController.saveProduct(prlc)) must_== BAD_REQUEST
      
      val products = Await.result(ProductDao.findAll(0, 10), Duration(5, SECONDS))
      products must haveSize(1)

      val product = products.head.as[Product]
      product.code must_== "GOOG"
      product.description must_== "Description"
    }
    
    "save a product using uppercase code" in withMongoDb { implicit app =>
      val pr = FakeRequest().withBody(Json.obj("code" -> "goog",  "description" -> "Description"))
      
      status(ProductController.saveProduct(pr)) must_== CREATED
      
      val products = Await.result(ProductDao.findAll(0, 10), Duration(5, SECONDS))
      products must haveSize(1)

      val product = products.head.as[Product]
      product.code must_== "GOOG"
      product.description must_== "Description"
    }
    
    "find a product with different case" in withMongoDb { implicit app =>
      val pr = FakeRequest().withBody(Json.obj("code" -> "GOOG",  "description" -> "Description"))
      
      status(ProductController.saveProduct(pr)) must_== CREATED
      
      val exists = Await.result(ProductDao.exists("code", "gOOg"), Duration(5, SECONDS))
      exists must_== true

      val products = Await.result(ProductDao.find("code", "goog"), Duration(5, SECONDS))
      products must haveSize(1)

      val product = products.head.as[Product]
      product.code must_== "GOOG"
      product.description must_== "Description"
    }
    
    "delete a product without transactions" in withMongoDb { implicit app =>
       val pr = FakeRequest().withBody(Json.obj("code" -> "GOOG",  "description" -> "Description"))
       status(ProductController.saveProduct(pr)) must_== CREATED 
       
       val productsBefore = Await.result(ProductDao.findAll(0, 10), Duration(5, SECONDS))
       productsBefore must haveSize(1)
       
       status(ProductController.deleteProduct(FakeRequest().withBody(
        Json.toJson(productsBefore.head))
       )) must_== OK
      
       val productsAfter = Await.result(ProductDao.findAll(0, 10), Duration(5, SECONDS))
       productsAfter must haveSize(0)
    }

    "not delete a product with a transaction" in withMongoDb { implicit app =>
       val productForm = FakeRequest().withBody(Json.obj("code" -> "GOOG",  "description" -> "Description"))
       status(ProductController.saveProduct(productForm)) must_== CREATED  

       val platformform = FakeRequest().withBody(Json.obj("name" -> "HSBC",  "currency" -> "CYN"))
       status(PlatformController.savePlatform(platformform)) must_== CREATED  
       
       Await.result(TransactionDao.save(Transaction
         (BSONObjectID.generate, new Date(), -1, "HSBC", "GOOG", 0, 1000, 700, Some(Buy), 0)), Duration(5, SECONDS))
       
       val productsBeforeDelete = Await.result(ProductDao.findAll(0, 10), Duration(5, SECONDS))
       productsBeforeDelete must haveSize(1)
       
       status(ProductController.deleteProduct(FakeRequest().withBody(
        Json.toJson(productsBeforeDelete.head))
       )) must_== BAD_REQUEST
      
       val productAfterDelete = Await.result(ProductDao.findAll(0, 10), Duration(5, SECONDS))
       productAfterDelete must haveSize(1)
    }
    
  }
 
}