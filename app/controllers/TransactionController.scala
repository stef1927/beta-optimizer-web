package controllers

import play.api.mvc._
import play.api.libs.json.Json
import play.api.libs.concurrent.Execution.Implicits._
import play.api.Logger
import models._
import services.TransactionDao
import reactivemongo.bson.BSONObjectID
import reactivemongo.bson.BSONDateTime
import java.util.Date

object TransactionController extends Controller {

  def getTransactions(product: String, page: Int, perPage: Int) = Action { implicit req =>
    Async {
      for {
        count <- TransactionDao.count
        transactions <- TransactionDao.findAll(product, page, perPage)
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
              "<" + routes.TransactionController.getTransactions(product, p, perPage).absoluteURL() + ">; rel=\"" + rel + "\""
          }.mkString(", "))
        }
      }
    }
  }

  case class TransactionForm(product: String, platform: String, exchange: Int, quantity: Int, 
      price: BigDecimal, side: String, cost: BigDecimal) {
    
    def toTransaction: Transaction = {
      val now = new Date
      val user = -1
      Transaction(BSONObjectID.generate, now, user, platform, product, exchange, quantity, price, Side.parse(side), cost)
    }
  }

  implicit val transactionFormFormat = Json.format[TransactionForm]
  
  def saveTransaction = Action(parse.json) { req =>
    Logger.info("Save transaction " + req.body)
    Json.fromJson[TransactionForm](req.body).fold(
      invalid => BadRequest("Bad transaction data"),
      form => Async {
        TransactionDao.save(form.toTransaction).map(_ => Created).recover { 
          case e => BadRequest(e.getMessage) 
        }
      }
    )
  }

  def deleteTransaction = Action(parse.json) { req =>
    Logger.info("Delete transaction " + req.body)
    Json.fromJson[Transaction](req.body).fold(
      invalid => BadRequest("Bad transaction data"),
      transaction => Async {
        TransactionDao.delete(transaction).map(_ => Ok).recover { 
          case e => BadRequest(e.getMessage) 
        }
      }
    )
  }
  
}