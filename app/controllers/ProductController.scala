package controllers

import play.api.mvc._
import play.api.libs.json.Json
import play.api.libs.concurrent.Execution.Implicits._
import play.api.Logger
import models._
import services.ProductDao
import reactivemongo.bson.BSONObjectID
import reactivemongo.bson.BSONDateTime

object ProductController extends Controller {

  def getProducts(page: Int, perPage: Int) = Action { implicit req =>
    Async {
      for {
        count <- ProductDao.count
        products <- ProductDao.findAll(page, perPage)
      } yield {
        val result = Ok(Json.toJson(products))

        // Calculate paging headers, if necessary
        val next = if (count > (page + 1) * perPage) Some("next" -> (page + 1)) else None
        val prev = if (page > 0) Some("prev" -> (page - 1)) else None
        val links = next ++ prev
        if (links.isEmpty) {
          result
        } else {
          result.withHeaders("Link" -> links.map {
            case (rel, p) =>
              "<" + routes.ProductController.getProducts(p, perPage).absoluteURL() + ">; rel=\"" + rel + "\""
          }.mkString(", "))
        }
      }
    }
  }

  def createIfNotExists(code: String, product: Product) {
    ProductDao.exists(code).map( {e => if(!e) ProductDao.save(product)} )
  }

}