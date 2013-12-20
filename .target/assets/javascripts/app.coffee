require(["webjars!knockout.js", 'webjars!jquery.js', "/routes.js", "webjars!bootstrap.js"], (ko) ->

  transactionsPerPage = 10

  class TransactionsModel
    constructor: () ->
      self = @
      
      @transactions = ko.observableArray()

      @productField = ko.observable()

      @quantityField = ko.observable()

      @nextTransactionsUrl = ko.observable()

      @prevTransactionsUrl = ko.observable()

      @saveTransaction = () ->
        @ajax(routes.controllers.TransactionController.saveTransaction(), {
          data: JSON.stringify({
            product: @productField()
            exchange: 0
            quantity: @quantityField()
            price: 100.50 
            side: "Buy"
            cost: 0
          })
          contentType: "application/json"
        }).done(() ->
          $("#addTransactionModal").modal("hide")
          self.productField(null)
          self.quantityField(null)
        )

      @getTransactions = () ->
        @ajax(routes.controllers.TransactionController.getTransactions(0, transactionsPerPage))
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

    # Convenience ajax request function
    ajax: (route, params) ->
      $.ajax($.extend(params, route))

    loadTransactions: (data, status, xhr) ->
      @transactions(data)

      # Link handling for paging
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

  model = new TransactionsModel
  ko.applyBindings(model)
  
  model.getTransactions()

  # Server Sent Events handling
  events = new EventSource(routes.controllers.MainController.events().url)
  events.addEventListener("transaction", (t) ->
    # Only add the data to the list if we're on the first page
    if model.prevTransactionsUrl() == null
      transaction = JSON.parse(t.data)
      model.transactions.unshift(transaction)
      
      if model.transactions().length > transactionsPerPage
        model.transactions.pop()
  , false)
)
