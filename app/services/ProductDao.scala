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

object ProductDao extends MongoCollection {

  def collectionName = "products"

  def eventCreated = "productCreated"

  def eventDeleted = "productDeleted"

  def save(product: Product): Future[Product] =
    save(product, List(notexists("code", product.code)))

  def delete(product: Product): Future[Product] =
    delete(product, product._id, 
      List(TransactionDao.notexists("product", product.code)))

}
