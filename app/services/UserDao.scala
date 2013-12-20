package services

import play.modules.reactivemongo.ReactiveMongoPlugin
import play.modules.reactivemongo.json.collection.JSONCollection
import play.modules.reactivemongo.json.BSONFormats._
import play.api.Play.current
import models._
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.Json
import reactivemongo.api.QueryOpts
import reactivemongo.core.commands.Count
import reactivemongo.bson.BSONObjectID

object UserDao {

  private def collection = ReactiveMongoPlugin.db.collection[JSONCollection]("users")

  def save(user: User): Future[User] = {
    collection.save(user).map {
      case ok if ok.ok =>
        EventDao.publish("user", user)
        user
      case error => throw new RuntimeException(error.message)
    }
  }

  def findAll(page: Int, perPage: Int): Future[Seq[User]] = {
    collection.find(Json.obj())
      .options(QueryOpts(skipN = page * perPage))
      .sort(Json.obj("_id" -> -1))
      .cursor[User]
      .toList(perPage)
  }

  def count: Future[Int] = {
    ReactiveMongoPlugin.db.command(Count(collection.name))
  }

}
