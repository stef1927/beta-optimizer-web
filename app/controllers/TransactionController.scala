package controllers

import play.api.mvc._
import play.api.libs.json.Json
import play.api.libs.concurrent.Execution.Implicits._
import play.api.Logger
import models._
import services.TransactionDao
import services.ProductDao
import reactivemongo.bson.BSONObjectID
import reactivemongo.bson.BSONDateTime

object TransactionController extends Controller {

  def getTransactions(page: Int, perPage: Int) = Action { implicit req =>
    Async {
      for {
        count <- TransactionDao.count
        transactions <- TransactionDao.findAll(page, perPage)
      } yield {
        val result = Ok(Json.toJson(transactions))

        // Calculate paging headers, if necessary
        val next = if (count > (page + 1) * perPage) Some("next" -> (page + 1)) else None
        val prev = if (page > 0) Some("prev" -> (page - 1)) else None
        val links = next ++ prev
        if (links.isEmpty) {
          result
        } else {
          result.withHeaders("Link" -> links.map {
            case (rel, p) =>
              "<" + routes.TransactionController.getTransactions(p, perPage).absoluteURL() + ">; rel=\"" + rel + "\""
          }.mkString(", "))
        }
      }
    }
  }

  case class TransactionForm(product: String, exchange: Int, quantity: Int, 
      price: BigDecimal, side: String, cost: BigDecimal) {
    
    def toTransaction: Transaction = {
      val now = new BSONDateTime(System.currentTimeMillis())
      val user = -1
      Transaction(BSONObjectID.generate, now, user, product, exchange, quantity, price, Side.parse(side), cost)
    }
    
    def toProduct: Product = {
      Product(BSONObjectID.generate, product, "Product description MISSING", List[Exchange]())
    }
      
  }

  implicit val transactionFormFormat = Json.format[TransactionForm]
  
  def saveTransaction = Action(parse.json) { req =>
    Logger.info("Save transaction " + req.body)
    Json.fromJson[TransactionForm](req.body).fold(
      invalid => BadRequest("Bad transaction data"),
      form => Async {
        ProductController.createIfNotExists(form.product, form.toProduct)
        TransactionDao.save(form.toTransaction).map(_ => Created)
      }
    )
  }

}