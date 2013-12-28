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

object TransactionDao extends MongoCollection {

  def collectionName = "transactions"

  def eventCreated = "transactionCreated"

  def eventDeleted = "transactionDeleted"

  def save(transaction: Transaction) : Future[Transaction] =
    save(transaction, List(
      ProductDao.exists("code", transaction.product),
      PlatformDao.exists("name", transaction.platform)
    ))

  def delete(transaction: Transaction): Future[Transaction] =   
    delete(transaction, transaction._id)
}
