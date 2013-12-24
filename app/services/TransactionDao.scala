package services

import play.modules.reactivemongo.ReactiveMongoPlugin
import play.modules.reactivemongo.json.collection.JSONCollection
import play.modules.reactivemongo.json.BSONFormats._
import play.api.Play.current
import play.api.Logger
import models._
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.Json
import reactivemongo.api.QueryOpts
import reactivemongo.core.commands.Count
import reactivemongo.bson.BSONObjectID
import reactivemongo.bson.BSONDocument

object TransactionDao {

  private def collection = ReactiveMongoPlugin.db.collection[JSONCollection]("transactions")

  def save(transaction: Transaction) : Future[Transaction] = {
    val op = ProductDao.exists(transaction.product).flatMap(exists => 
      if (exists) 
        TransactionDao.saveNoCheck(transaction) 
      else {
        val err = "Product does not exist"
        Logger.info(err)
        throw new RuntimeException(err)  
      })
    
    op.recover { case ex => 
      val err = "Failed to save transaction: " + ex.getMessage
      Logger.error(err)
      throw new RuntimeException(err)
    }
  }

  def saveNoCheck(transaction: Transaction): Future[Transaction] = {
    Logger.info("Save transaction " + transaction)
    
    collection.save(transaction).map {
      case ok if ok.ok =>
        EventDao.publish("transactionCreated", transaction)
        transaction
      case error => 
        Logger.error("Failed to save: " + error.message)
        throw new RuntimeException(error.message)
    }
  }
  
  def delete(transaction: Transaction): Future[Transaction] = {
    Logger.info("Delete transaction " + transaction)
    
    collection.remove(BSONDocument("_id" -> transaction._id), firstMatchOnly=true).map {
      case ok if ok.ok =>
        EventDao.publish("transactionDeleted", transaction)
        transaction
      case error => 
        Logger.error("Failed to delete: " + error.message)
        throw new RuntimeException(error.message)
    }
  }

  def findAll(page: Int, perPage: Int): Future[Seq[Transaction]] = findAll("", page, perPage)
  
  def findAll(product: String, page: Int, perPage: Int): Future[Seq[Transaction]] = {
    Logger.info("Find transactions " + product)
     
    val query = if (product.isEmpty()) BSONDocument() else BSONDocument("product" -> product)
    collection.find(query)
      .options(QueryOpts(skipN = page * perPage))
      .sort(Json.obj("_id" -> -1))
      .cursor[Transaction]
      .toList(perPage)
  }

  def count: Future[Int] = {
    Logger.info("Count transactions")
    ReactiveMongoPlugin.db.command(Count(collection.name))
  }

}
