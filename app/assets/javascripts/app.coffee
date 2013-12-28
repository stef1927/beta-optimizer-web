require(["webjars!knockout.js", "viewmodel", "webjars!jquery.js", "/routes.js", "webjars!bootstrap.js"], (ko, ViewModel) ->

  model = new ViewModel()
  ko.applyBindings(model)
  
  # Server Sent Events handling
  events = new EventSource(routes.controllers.MainController.events().url)
  events.addEventListener("transactionCreated", (t) ->
      model.onTransactionCreated(JSON.parse(t.data))
  , false)

  events.addEventListener("transactionDeleted", (t) ->
    model.onTransactionDeleted(JSON.parse(t.data))
  , false)

  events.addEventListener("productCreated", (p) ->
    model.onProductCreated(JSON.parse(p.data))
  , false)

  events.addEventListener("productDeleted", (p) ->
    model.onProductDeleted(JSON.parse(p.data))
  , false)

  events.addEventListener("platformCreated", (p) ->
    model.onPlatformCreated(JSON.parse(p.data))
  , false)

  events.addEventListener("platformDeleted", (p) ->
   model.onPlatformDeleted(JSON.parse(p.data))
  , false)
)
