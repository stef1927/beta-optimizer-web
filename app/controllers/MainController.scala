package controllers

import play.api.mvc._
import play.api.Routes
import services.EventDao
import play.api.libs.EventSource

object MainController extends Controller {

  def index(path: String) = Action {
    Ok(views.html.index())
  }

  def router = Action { implicit req =>
    Ok(
      Routes.javascriptRouter("routes")(
        routes.javascript.MainController.events,
        routes.javascript.TransactionController.getTransactions,
        routes.javascript.TransactionController.saveTransaction,
        routes.javascript.TransactionController.deleteTransaction,
        routes.javascript.ProductController.getProducts,
        routes.javascript.ProductController.saveProduct,
        routes.javascript.ProductController.deleteProduct
      )
    ).as("text/javascript")
  }

  def events = Action {
    Ok.feed(EventDao.stream &> EventSource()).as(EVENT_STREAM)
  }
}
