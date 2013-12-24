require(["webjars!knockout.js", 'webjars!jquery.js', "/routes.js", "webjars!bootstrap.js"], (ko) ->

  rowsPerPage = 10
  alertTimeoutSeconds = 3

  class DumbTraderModel
    constructor: () ->
      self = @
      
      #
      # Utility functions
      #

      # Convert to a number rounded to precision digits
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

      # Check if same BSON object  
      @isSame = (a, b) ->
        #if (a != undefined && b != undefined)
        #  self.log("a._id: " + JSON.stringify(a) + " - b._id: " + JSON.stringify(b))
        a != undefined && b != undefined && JSON.stringify(a) == JSON.stringify(b)

      #
      # Modal dialogs
      #

      @deleteProductDialog = () ->
        @showModal("Delete Product?", "Really delete the selected product?", "Delete", @deleteProduct)  

      @deleteTransactionDialog = () ->
        @showModal("Delete Transaction?", "Really delete the selected transaction?", "Delete", @deleteTransaction)  

      @modalQuestion = ko.observable()
      @showModal = (header, body, action, doAction) ->
        @modalQuestion({
          header: header,
          body: body,
          action: action,
          doAction: doAction
        })
        $('#modalQuestion').modal('show');

      @errorMessage = ko.observable(null)
      @errorVisible = ko.computed(() -> self.errorMessage() != null)
      
      @showError = (msg) ->
        @errorMessage(msg)
        setTimeout(()-> 
          self.dismissError()
        , alertTimeoutSeconds * 1000)
    
      @dismissError = () ->
        @errorMessage(null) 

      @infoMessage = ko.observable(null)
      @infoVisible = ko.computed(() -> self.infoMessage() != null)
      
      @showInfo = (msg) ->
        @infoMessage(msg)
        setTimeout(()-> 
          self.dismissInfo()
        , alertTimeoutSeconds * 1000)
    
      @dismissInfo = () ->
        @infoMessage(null)    

      #
      # Main page switch
      #
      @mainPage = ko.observable("transactions")
      
      @showTransactions = () ->
        @mainPage("transactions")

      @showHoldings = () ->
        @mainPage("holdings")

      #
      # Transactions
      #

      @transactions = ko.observableArray()
      @platformField = ko.observable()
      @quantityField = ko.observable().extend(numeric: 0)
      @priceField = ko.observable().extend(numeric: 4)
      
      @actions = ko.observableArray(["Buy", "Sell"])
      @selectedAction = ko.observable()

      @nextTransactionsUrl = ko.observable()
      @prevTransactionsUrl = ko.observable()

      @selectedTransaction = ko.observable()

      @formatDate = (timestamp) ->
        ret = new Date(timestamp) 
        ret.getDate() + '/' + (ret.getMonth() + 1) + '/' + ret.getFullYear()   

      @saveTransaction = () ->
        productExists = ko.utils.arrayFirst(@products(), (p) -> 
          p.code == self.productField()
        )
        
        unless productExists
          @productDescriptionField("Description MISSING")
          @saveProduct()

        @ajax(routes.controllers.TransactionController.saveTransaction(), {
          data: JSON.stringify({
            product: @productField()
            platform: @platformField()
            quantity: @quantityField()
            price: @priceField()
            side: @selectedAction()
            exchange: 0
            cost: 0
          })
          contentType: "application/json"
        }).done(() ->
          $("#saveTransactionModal").modal("hide")
          self.showInfo("Transaction created")
        ).fail((req, status, err) ->
          self.showError(req.responseText)
        )

       @deleteTransaction = () ->
        self.ajax(routes.controllers.TransactionController.deleteTransaction(), {
          data: JSON.stringify(self.selectedTransaction())
          contentType: "application/json"
        }).done(() ->
          self.showInfo("Transaction deleted")
        ).fail((req, status, err) ->
          self.showError(req.responseText)
        )  

      @selectTransaction = (transaction) ->
        self.selectedTransaction(transaction)

      @isSelectedTransaction = (transaction)->
        self.isSame(transaction, self.selectedTransaction())  

      @getTransactions = () -> 
        getTransactions("")
        
      @getTransactions = (code) ->
        @ajax(routes.controllers.TransactionController.getTransactions(code, 0, rowsPerPage))
          .done((data, status, xhr) ->
            self.loadTransactions(data, status, xhr)
          )
          .fail((req, status, err) ->
            self.showError(req.responseText)
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

      #
      # Products
      #

      @products = ko.observableArray()   
      @productField = ko.observable()
      @productDescriptionField = ko.observable() 
      @selectedProduct = ko.observable()
      @nextProductsUrl = ko.observable()
      @prevProductsUrl = ko.observable()

      @saveProduct = () ->
        @ajax(routes.controllers.ProductController.saveProduct(), {
          data: JSON.stringify({
            code: @productField()
            description: @productDescriptionField()
          })
          contentType: "application/json"
        }).done(() ->
          $("#saveProductModal").modal("hide")
          self.showInfo("Product created")
        ).fail((req, status, err) ->
          self.showError(req.responseText)
        )

       @deleteProduct = () ->
        self.ajax(routes.controllers.ProductController.deleteProduct(), {
          data: JSON.stringify(self.selectedProduct())
          contentType: "application/json"
        }).done(() ->
          self.showInfo("Product deleted")
        ).fail((req, status, err) ->
          self.showError(req.responseText)
        )  

      @selectProduct = (product) ->
        self.selectedProduct(product)
        self.getTransactions(product.code)

      @resetProduct = () ->
        self.selectedProduct(undefined)
        self.getTransactions()

      @isSelectedProduct = (product)->
        self.isSame(product, self.selectedProduct())

      @getProducts = () ->
        @ajax(routes.controllers.ProductController.getProducts(0, rowsPerPage))
          .done((data, status, xhr) ->
            self.loadProducts(data, status, xhr)
          ).fail((req, status, err) ->
            self.showError(req.responseText)
          )

      @nextProducts = () ->
        if @nextProductsUrl()
          $.ajax({url: @nextProductsUrl()}).done((data, status, xhr) ->
            @loadProducts(data, status, xhr)
          )

      @prevProducts = () ->
        if @prevProductsUrl()
          $.ajax({url: @prevProductsUrl()}).done((data, status, xhr) ->
            @loadProducts(data, status, xhr)
          )    

    log: (msg) ->
      setTimeout(() -> 
        throw new Error(msg)
      , 0)

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

  events.addEventListener("transactionDeleted", (t) ->
    transaction = JSON.parse(t.data)
    model.transactions.remove((t) -> model.isSame(t, transaction) )
  , false)

  events.addEventListener("productCreated", (p) ->
    if model.prevTransactionsUrl() == null
      product = JSON.parse(p.data)
      model.products.unshift(product)
      
      if model.products().length > rowsPerPage
        model.products.pop()
  , false)

  events.addEventListener("productDeleted", (p) ->
    product = JSON.parse(p.data)
    model.products.remove((p) -> model.isSame(p, product))
  , false)
)
