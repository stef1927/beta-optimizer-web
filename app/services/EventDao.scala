package services

import play.api._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.iteratee._
import play.api.libs.json.{Json, Format, JsValue}

import play.modules.reactivemongo.ReactiveMongoPlugin
import play.modules.reactivemongo.json.collection.JSONCollection
import play.modules.reactivemongo.json.BSONFormats._

import reactivemongo.api.QueryOpts
import reactivemongo.bson.BSONObjectID
import reactivemongo.api.indexes.{IndexType, Index}

import scala.concurrent.Future
import models.Event


object EventDao {

  def publish[T: Format](name: String, data: T): Future[Event] = publish(name, Json.toJson(data))

  def publish(name: String, data: JsValue): Future[Event] = {
    val event = new Event(BSONObjectID.generate, name, data)
    for {
      coll <- plugin.collection
      result <- coll.save(event)
    } yield {
      Logger.info("Published event " + event + " with result " + result.ok)
      result match {
        case ok if result.ok => event
        case error => throw new RuntimeException(error.message)
      }
    }
  }

  lazy val stream: Enumerator[Event] = plugin.stream

  private def plugin = (for {
    app <- Play.maybeApplication
    plugin <- app.plugin[EventPlugin]
  } yield plugin).getOrElse(sys.error("Could not load event plugin"))
}

class EventPlugin(app: Application) extends Plugin {

  lazy val collection: Future[JSONCollection] = {
    Logger.info("Creating events collection")
    val coll = ReactiveMongoPlugin.db(app)[JSONCollection]("events")
    coll.stats().flatMap {
      case stats if !stats.capped =>
        // The collection is not capped, so we convert it
        coll.convertToCapped(102400, Some(1000))
      case _ => Future.successful(true)
    }.recover {
      // The collection mustn't exist, create it
      case _ =>
        coll.createCapped(102400, Some(1000))
    }.map { _ =>
      coll.indexesManager.ensure(Index(
        key = Seq("_id" -> IndexType.Ascending),
        unique = true
      ))
      coll
    }
  }

  lazy val stream: Enumerator[Event] = {
    
    // ObjectIDs are linear with time, we only want events created after now.
    val since = BSONObjectID.generate
    Logger.info("Creating enumerator since " + since)
    val enumerator = Enumerator.flatten(for {
      coll <- collection
    } yield coll.find(Json.obj("_id" -> Json.obj("$gt" -> since)))
        .options(QueryOpts().tailable.awaitData)
        .cursor[Event]
        .enumerate()
    )
    Concurrent.broadcast(enumerator)._1
  }
}