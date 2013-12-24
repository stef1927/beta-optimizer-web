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
object TransactionControllerSpec extends Specification {

  "the transaction controller" should {
	  
    "save a transaction" in withMongoDb { implicit app =>
      createProduct("GOOG", "Google")
      status(TransactionController.saveTransaction(FakeRequest().withBody(
        Json.obj("product" -> "GOOG", 
                 "platform" -> "HSBC",
        		 "exchange" -> 0, 
        		 "quantity" -> 100, 
        		 "price" -> 50.50, 
        		 "side" -> "Buy", 
        		 "cost" -> 0)
      ))) must_== CREATED
      val transactions = Await.result(TransactionDao.findAll(0, 10), Duration(5, SECONDS))
      transactions must haveSize(1)
      transactions.head.product must_== "GOOG"
      transactions.head.exchange must_== 0
      transactions.head.quantity must_== 100
      transactions.head.price must_== 50.50
      transactions.head.side must_== Some(Buy)
      transactions.head.cost must_== 0
    }
    
    "not save a transaction without product" in withMongoDb { implicit app =>
      status(TransactionController.saveTransaction(FakeRequest().withBody(
        Json.obj("product" -> "GOOG", 
                 "platform" -> "HSBC",
        		 "exchange" -> 0, 
        		 "quantity" -> 100, 
        		 "price" -> 50.50, 
        		 "side" -> "Buy", 
        		 "cost" -> 0)
      ))) must_== BAD_REQUEST
      val transactions = Await.result(TransactionDao.findAll(0, 10), Duration(5, SECONDS))
      transactions must haveSize(0)
    }
    
    "delete a transaction" in withMongoDb { implicit app =>
      createProduct("GOOG", "Google")
      createTransaction("GOOG", "HSBC", 0, 200, 123, "Buy", 0)
      createTransaction("GOOG", "HSBC", 0, 300, 345, "Sell", 0)
      val transactionsBeforeDelete = Await.result(TransactionDao.findAll(0, 10), Duration.Inf)
      transactionsBeforeDelete must haveSize(2)
      
      status(TransactionController.deleteTransaction(FakeRequest().withBody(
        Json.toJson(transactionsBeforeDelete.head))
      )) must_== OK
      
      val transactionsAfterDelete = Await.result(TransactionDao.findAll(0, 10), Duration.Inf)
      transactionsAfterDelete must haveSize(1)
      transactionsAfterDelete.head.side must_== Some(Buy)
    }

    "get transactions" in withMongoDb { implicit app =>
      createProduct("GOOG", "Google")
      createProduct("BP", "BP descr")
      createTransaction("GOOG", "HSBC", 0, 400, 789, "Buy", 0)
      createTransaction("BP", "HSBC", 0, 500, 981, "Sell", 0)
      val transactions = Json.parse(contentAsString(TransactionController.getTransactions("", 0, 10)(FakeRequest()))).as[Seq[Transaction]]
      transactions must haveSize(2)
      transactions(0).product must_== "BP"
      transactions(1).product must_== "GOOG"
    }
    
    "get transactions for a product" in withMongoDb { implicit app =>
      createProduct("GOOG", "Google")
      createProduct("BP", "BP descr")
      createTransaction("GOOG", "HSBC", 0, 600, 543, "Buy", 0)
      createTransaction("BP", "HSBC", 0, 700, 321, "Sell", 0)
      val transactions = Json.parse(contentAsString(TransactionController.getTransactions("GOOG", 0, 10)(FakeRequest()))).as[Seq[Transaction]]
      transactions must haveSize(1)
      transactions(0).product must_== "GOOG"
    }
    
    "page transactions" in withMongoDb { implicit app =>
      createProduct("GOOG", "Google")
      for (i <- 1 to 30) {
         createTransaction("GOOG", "HSBC", 0, 800, 500, "Buy", 0)
      }
      def test(page: Int, perPage: Int) = {
        val result = TransactionController.getTransactions("", page, perPage)(FakeRequest())
        val (prev, next) = header("Link", result).map { link =>
          (extractLink("prev", link), extractLink("next", link))
        }.getOrElse((None, None))
        (prev.isDefined, next.isDefined, Json.parse(contentAsString(result)).as[Seq[Transaction]].size)
      }

      test(0, 10) must_== (false, true, 10)
      test(1, 10) must_== (true, true, 10)
      test(2, 10) must_== (true, false, 10)
      test(3, 10) must_== (true, false, 0)
      test(0, 30) must_== (false, false, 30)
      test(0, 31) must_== (false, false, 30)
      test(0, 29) must_== (false, true, 29)
    }
  }

  def createProduct(code: String, description: String) = {
    val now = new Date()
    Await.result(ProductDao.save(Product
     (BSONObjectID.generate, code, description, 0, now)), Duration(5, SECONDS))
  }
  
  def createTransaction(product: String, platform: String, exchange: Int, quantity: Int, 
      price: BigDecimal, side: String, cost: BigDecimal) = {
    val now = new Date()
    Await.result(TransactionDao.save(Transaction
     (BSONObjectID.generate, now, -1, platform, product, 
      exchange, quantity, price, Side.parse(side), cost)), Duration(5, SECONDS))
  }

  def extractLink(rel: String, link: String) = {
    """<([^>]*)>;\s*rel="%s"""".format(rel).r.findFirstMatchIn(link).map(_.group(1))
  }
}
