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
import reactivemongo.bson.BSONDateTime

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import MongoDBTestUtils.withMongoDb

@RunWith(classOf[JUnitRunner])
object TransactionControllerSpec extends Specification {

  "the message controller" should {

    "save a transaction" in withMongoDb { implicit app =>
      status(TransactionController.saveTransaction(FakeRequest().withBody(
        Json.obj("product" -> "GOOG", 
        		 "exchange" -> 0, 
        		 "quantity" -> 100, 
        		 "price" -> 100.50, 
        		 "side" -> "Buy", 
        		 "cost" -> 0)
      ))) must_== CREATED
      val transactions = Await.result(TransactionDao.findAll(0, 10), Duration.Inf)
      transactions must haveSize(1)
      transactions.head.product must_== "GOOG"
      transactions.head.exchange must_== 0
      transactions.head.quantity must_== 100
      transactions.head.price must_== 100.50
      transactions.head.side must_== Some(Buy)
      transactions.head.cost must_== 0
    }

    "get transactions" in withMongoDb { implicit app =>
      createTransaction("GOOG", 0, 100, 100.50, "Buy", 0)
      createTransaction("BP", 0, 100, 100.50, "Sell", 0)
      val transactions = Json.parse(contentAsString(TransactionController.getTransactions(0, 10)(FakeRequest()))).as[Seq[Transaction]]
      transactions must haveSize(2)
      transactions(0).product must_== "BP"
      transactions(1).product must_== "GOOG"
    }

    "page transactions" in withMongoDb { implicit app =>
      for (i <- 1 to 30) {
         createTransaction("GOOG", 0, 100, 100.50, "Buy", 0)
      }
      def test(page: Int, perPage: Int) = {
        val result = TransactionController.getTransactions(page, perPage)(FakeRequest())
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

  def createTransaction(product: String, exchange: Int, quantity: Int, 
      price: BigDecimal, side: String, cost: BigDecimal) = {
    val now = new BSONDateTime(System.currentTimeMillis())
    Await.result(TransactionDao.save(Transaction
     (BSONObjectID.generate, now, -1, product, exchange, quantity, price, Side.parse(side), cost)), Duration.Inf)
  }

  def extractLink(rel: String, link: String) = {
    """<([^>]*)>;\s*rel="%s"""".format(rel).r.findFirstMatchIn(link).map(_.group(1))
  }
}
