require(["webjars!knockout.js", 'webjars!jquery.js', "/routes.js", "webjars!bootstrap.js"], (ko) ->

  rowsPerPage = 10

  class DumbTraderModel
    constructor: () ->
      self = @
      
      ko.extenders.numeric = (target, precision) ->
        result = ko.computed(
          read: target
          write: (newValue) ->
            current = target()
            roundingMultiplier = Math.pow(10, precision)
            newValueAsNum = (if isNaN(newValue) then 0 else parseFloat(+newValue))
            valueToWrite = Math.round(newValueAsNum * roundingMultiplier) / roundingMultiplier
            if valueToWrite isnt current
              target valueToWrite
            else
              target.notifySubscribers valueToWrite  if newValue isnt current
        ).extend(notify: "always")
        result target()
        result

      @transactions = ko.observableArray()
      @productField = ko.observable()
      @quantityField = ko.observable().extend(numeric: 0)
      @priceField = ko.observable().extend(numeric: 4)
      
      @actions = ko.observableArray(["Buy", "Sell"])
      @selectedAction = ko.observable()

      @nextTransactionsUrl = ko.observable()
      @prevTransactionsUrl = ko.observable()

      @formatDate = (timestamp) ->
        ret = new Date JSON.stringify Date(timestamp) 
        ret.getDate() + '/' + (ret.getMonth() + 1) + '/' + ret.getFullYear()   

      @saveTransaction = () ->
        @ajax(routes.controllers.TransactionController.saveTransaction(), {
          data: JSON.stringify({
            product: @productField()
            quantity: @quantityField()
            price: @priceField()
            side: @selectedAction()
            exchange: 0
            cost: 0
          })
          contentType: "application/json"
        }).done(() ->
          $("#addTransactionModal").modal("hide")
          self.productField(null)
          self.quantityField(null)
        ).fail((req, status, err) ->
          alert("Request failed: " + err)
        )

      @getTransactions = (product) ->
        @ajax(routes.controllers.TransactionController.getTransactions(product.code, 0, rowsPerPage))
          .done((data, status, xhr) ->
            self.loadTransactions(data, status, xhr)
          )

      @nextTransactions = () ->
        if @nextTransactionsUrl()
          $.ajax({url: @nextTransactionsUrl()}).done((data, status, xhr) ->
            self.loadTransactions(data, status, xhr)
          )

      @prevTransactions = () ->
        if @prevTransactionsUrl()
          $.ajax({url: @prevTransactionsUrl()}).done((data, status, xhr) ->
            self.loadTransactions(data, status, xhr)
          )

      @products = ko.observableArray()    
      @nextProductsUrl = ko.observable()
      @prevProductsUrl = ko.observable()

      @getProducts = () ->
        @ajax(routes.controllers.ProductController.getProducts(0, rowsPerPage))
          .done((data, status, xhr) ->
            self.loadProducts(data, status, xhr)
          )

      @nextProducts = () ->
        if @nextProductsUrl()
          $.ajax({url: @nextProductsUrl()}).done((data, status, xhr) ->
            self.loadProducts(data, status, xhr)
          )

      @prevProducts = () ->
        if @prevProductsUrl()
          $.ajax({url: @prevProductsUrl()}).done((data, status, xhr) ->
            self.loadProducts(data, status, xhr)
          )    

    # Convenience ajax request function
    ajax: (route, params) ->
      $.ajax($.extend(params, route))

    loadTransactions: (data, status, xhr) ->
      @transactions(data)

      link = xhr.getResponseHeader("Link")
      if link
        next = /.*<([^>]*)>; rel="next".*/.exec(link)
        if next
          @nextTransactionsUrl(next[1])
        else
          @nextTransactionsUrl(null)
        prev = /.*<([^>]*)>; rel="prev".*/.exec(link)
        if prev
          @prevTransactionsUrl(prev[1])
        else
          @prevTransactionsUrl(null)
      else
        @nextTransactionsUrl(null)
        @prevTransactionsUrl(null)

    loadProducts: (data, status, xhr) ->
      @products(data)    

      link = xhr.getResponseHeader("Link")
      if link
        next = /.*<([^>]*)>; rel="next".*/.exec(link)
        if next
          @nextProductsUrl(next[1])
        else
          @nextProductsUrl(null)
        prev = /.*<([^>]*)>; rel="prev".*/.exec(link)
        if prev
          @prevProductsUrl(prev[1])
        else
          @prevProductsUrl(null)
      else
        @nextProductsUrl(null)
        @prevProductsUrl(null)

  model = new DumbTraderModel
  ko.applyBindings(model)
  
  model.getTransactions()
  model.getProducts()

  # Server Sent Events handling
  events = new EventSource(routes.controllers.MainController.events().url)
  events.addEventListener("transactionCreated", (t) ->
    if model.prevTransactionsUrl() == null
      transaction = JSON.parse(t.data)
      model.transactions.unshift(transaction)
      
      if model.transactions().length > rowsPerPage
        model.transactions.pop()
  , false)

  events.addEventListener("product", (p) ->
    if model.prevTransactionsUrl() == null
      product = JSON.parse(p.data)
      model.products.unshift(product)
      
      if model.products().length > rowsPerPage
        model.products.pop()
  , false)
)
