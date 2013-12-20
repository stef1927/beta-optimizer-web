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

  def save(product: Product) {
    collection.save(product).map {
      case ok if ok.ok =>
        EventDao.publish("product", product)
        product
      case error => throw new RuntimeException(error.message)
    }
  }

  def exists(code: String): Future[Boolean] = {
    find(code).map { list => list.size > 0 }
  }
  
  
  def find(code: String): Future[Seq[Product]] = {
    val query = BSONDocument("code" -> code)
    collection.find(query).cursor[Product].toList
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
