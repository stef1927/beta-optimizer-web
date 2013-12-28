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

object PlatformDao extends MongoCollection {

  def collectionName = "platforms"

  def eventCreated = "platformCreated"

  def eventDeleted = "platformDeleted"

  def save(platform: Platform): Future[Platform] =
    save(platform, List(notexists("name", platform.name)))

  def delete(platform: Platform): Future[Platform] =
    delete(platform, platform._id, 
      List(TransactionDao.notexists("platform", platform.name)))

}
