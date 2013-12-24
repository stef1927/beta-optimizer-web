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

object ProductDao {

  private def collection = ReactiveMongoPlugin.db.collection[JSONCollection]("products")

  def save(product: Product) : Future[Product] = {
    val op = ProductDao.exists(product.code).flatMap(exists => 
      if (!exists) 
        ProductDao.saveNoCheck(product) 
      else {
        val err = "Product already exists"
        Logger.info(err)
        throw new RuntimeException(err)  
      })
    
    op.recover { case ex => 
      val err = "Failed to save product: " + ex.getMessage
      Logger.error(err)
      throw new RuntimeException(err)
    }
  }

  def exists(code: String): Future[Boolean] = {
    find(code).map { list => list.size > 0 }
  }
  
  
  def find(code: String): Future[Seq[Product]] = {
    val query = BSONDocument("code" -> code.toUpperCase)
    collection.find(query).cursor[Product].toList
  }

  def saveNoCheck(product: Product) : Future[Product] = {
    collection.save(product).map {
      case ok if ok.ok =>
        EventDao.publish("productCreated", product)
        product
      case error => throw new RuntimeException(error.message)
    }
  }
  
  def delete(product: Product): Future[Product] = {
    Logger.info("Delete product " + product)
    
    val op = TransactionDao.findAll(product.code, 0, 10).flatMap(transactions =>
      if (transactions.isEmpty) 
        deleteNoCheck(product) 
      else {
        val err = "Transactions exists"
        Logger.info(err)
        throw new RuntimeException(err)  
      })
    
    op.recover{ 
      case ex => 
        val err = "Cannot delete product: " + ex.getMessage
        Logger.error(err)
        throw new RuntimeException(err)
    }
  }
  
  def deleteNoCheck(product: Product): Future[Product] = {
    Logger.info("Delete product no check " + product)
    collection.remove(BSONDocument("_id" -> product._id), firstMatchOnly=true).map {
      case ok if ok.ok =>
        EventDao.publish("productDeleted", product)
        product
      case error => 
        Logger.error("Failed to delete: " + error.message)
        throw new RuntimeException(error.message)
    }
  }

  def findAll(page: Int, perPage: Int): Future[Seq[Product]] = {
    collection.find(Json.obj())
      .options(QueryOpts(skipN = page * perPage))
      .sort(Json.obj("_id" -> -1))
      .cursor[Product]
      .toList(perPage)
  }

  def count: Future[Int] = {
    ReactiveMongoPlugin.db.command(Count(collection.name))
  }

}
