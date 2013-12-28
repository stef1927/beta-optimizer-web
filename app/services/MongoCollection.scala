package services

import play.modules.reactivemongo.ReactiveMongoPlugin
import play.modules.reactivemongo.json.collection.JSONCollection
import play.modules.reactivemongo.json.BSONFormats._
import play.api.Play.current
import play.api.Logger
import models._
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.{Json, Format, JsObject}
import reactivemongo.api.QueryOpts
import reactivemongo.core.commands.Count
import reactivemongo.bson.BSONObjectID
import reactivemongo.bson.BSONDocument

trait MongoCollection {

  def collectionName : String

  // The event published when an object is created
  def eventCreated: String

  // The event published when an object is deleted
  def eventDeleted: String

  def collection = ReactiveMongoPlugin.db.collection[JSONCollection](collectionName)

  def save[T: Format](obj: T, conditions: List[Future[Boolean]]) : Future[T] = {
    Future.fold(conditions)(true)(_ && _).flatMap(cond =>
      if(cond) save(obj)
      else throw new RuntimeException("condition failed"))
    .recover { case ex => 
      val err = "Failed to save object: " + ex.getMessage
      Logger.error(err)
      throw new RuntimeException(err)
    }
  }

  def save[T: Format](obj: T) : Future[T] = {
    collection.save(obj).map {
      case ok if ok.ok =>
        EventDao.publish(eventCreated, obj)
        obj
      case error => throw new RuntimeException(error.message)
    }
  }

  def delete[T: Format](obj: T, id: BSONObjectID, 
      conditions: List[Future[Boolean]]) : Future[T] = {
    Future.fold(conditions)(true)(_ && _).flatMap(cond =>
      if(cond) delete(obj, id)
      else throw new RuntimeException("condition failed"))
    .recover { case ex => 
      val err = "Failed to delete object: " + ex.getMessage
      Logger.error(err)
      throw new RuntimeException(err)
    }
  }

  def delete[T: Format](obj: T, id: BSONObjectID): Future[T] = {
    collection.remove(BSONDocument("_id" -> id), firstMatchOnly=true).map {
      case ok if ok.ok =>
        EventDao.publish(eventDeleted, obj)
        obj
      case error => 
        Logger.error("Failed to delete: " + error.message)
        throw new RuntimeException(error.message)
    }
  }

  def notexists(key: String, value: String): Future[Boolean] = 
    find(key, value).map { list => list.size == 0 }

  def exists(key: String, value: String): Future[Boolean] = 
    find(key, value).map { list => list.size > 0 }

  
  def find(key: String, value: String): Future[Seq[JsObject]] = {
    val query = BSONDocument(key -> value.toUpperCase)
    collection.find(query).cursor[JsObject].toList
  }

  def find(key: String, value: String, page: Int, perPage: Int): Future[Seq[JsObject]] = {
    val query = BSONDocument(key -> value.toUpperCase)
    collection.find(query)
    .options(QueryOpts(skipN = page * perPage))
    .sort(Json.obj("_id" -> -1))
    .cursor[JsObject].toList
  }

  def findAll(): Future[Seq[JsObject]] = {
    collection.find(Json.obj())
      .sort(Json.obj("_id" -> -1))
      .cursor[JsObject]
      .toList
  }

  def findAll(page: Int, perPage: Int): Future[Seq[JsObject]] = {
    collection.find(Json.obj())
      .options(QueryOpts(skipN = page * perPage))
      .sort(Json.obj("_id" -> -1))
      .cursor[JsObject]
      .toList(perPage)
  }

  def count: Future[Int] = {
    ReactiveMongoPlugin.db.command(Count(collection.name))
  }

}
