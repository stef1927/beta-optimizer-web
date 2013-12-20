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

object TransactionDao {

  private def collection = ReactiveMongoPlugin.db.collection[JSONCollection]("transactions")

  def save(transaction: Transaction): Future[Transaction] = {
    Logger.info("Save transaction " + transaction)
    
    collection.save(transaction).map {
      case ok if ok.ok =>
        EventDao.publish("transaction", transaction)
        Logger.info("Published transaction " + transaction)
        transaction
      case error => throw new RuntimeException(error.message)
    }
  }

  def findAll(page: Int, perPage: Int): Future[Seq[Transaction]] = {
    collection.find(Json.obj())
      .options(QueryOpts(skipN = page * perPage))
      .sort(Json.obj("_id" -> -1))
      .cursor[Transaction]
      .toList(perPage)
  }

  def count: Future[Int] = {
    ReactiveMongoPlugin.db.command(Count(collection.name))
  }

}
