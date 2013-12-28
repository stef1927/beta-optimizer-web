define(["webjars!knockout.js", 'webjars!jquery.js', "/routes.js", "webjars!bootstrap.js"], (ko) ->
  
  class ViewModel
    rowsPerPage = 10
    alertTimeoutSeconds = 5
    numDecimals = 2

    isSame: (a, b) ->
      a != undefined && b != undefined && JSON.stringify(a) == JSON.stringify(b)

    constructor: () ->
      #self = @

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

      #
      # Modal dialogs
      #
      @modalQuestion = ko.observable()
      @showModal = (header, body, action, doAction) ->
        @modalQuestion({
          header: header,
          body: body,
          action: action,
          doAction: doAction
        })
        $('#modalQuestion').modal('show');

      #
      # Alert messages
      #
      @messages = ko.observableArray()

      @showMessage = (text, style) ->
        msg = {
          text: text,
          style: style
        }
        @messages.push(msg)
        msg
    
      @dismissMessage = (msg) =>
        @messages.remove(msg)

      @showError = (text) ->
        @showMessage(text, 'alert-danger')

      @showInfo = (text) ->
        msg = @showMessage(text, 'alert-success')
        setTimeout(()=> 
          @dismissMessage(msg)
        , alertTimeoutSeconds * 1000)

      #
      # Main page switch
      #
      @mainPage = ko.observable("holdings")
      
      @showTransactions = () ->
        @mainPage("transactions")

      @showHoldings = () ->
        @mainPage("holdings")

      #
      # Transactions
      #

      @transactions = ko.observableArray()
      @quantityField = ko.observable().extend(numeric: 0)
      @priceField = ko.observable().extend(numeric: numDecimals)
      
      @actions = ko.observableArray(["Buy", "Sell"])
      @selectedAction = ko.observable()

      @nextTransactionsUrl = ko.observable()
      @prevTransactionsUrl = ko.observable()

      @selectedTransaction = ko.observable()

      @formatDate = (timestamp) ->
        ret = new Date(timestamp) 
        ret.getDate() + '/' + (ret.getMonth() + 1) + '/' + ret.getFullYear()   

      # Convert the price to a fixed number with trailing zeros, e.g.
      # 50.00 and then replace the extra zeros with non-breaking spaces
      # so that when the numbers are right aligned in a table, they will be
      # aligned to the decimal place.   
      @formatPrice = (price) ->
        ret = price.toFixed(numDecimals)
        m = (ret.match(/\.0+$/)||ret.match(/0+$/)||[])
        if m.length > 0
          len = m[0].length
          ret.slice(0, -len) + Array(len+1).join('\xA0') #&nbsp
        else
          ret

      @saveTransaction = () ->
        productExists = ko.utils.arrayFirst(@products(), (p) => 
          p.code == @productField().toUpperCase()
        )
        
        unless productExists
          @productDescriptionField("Description MISSING")
          @saveProduct()

        @ajax(routes.controllers.TransactionController.saveTransaction(), {
          data: JSON.stringify({
            product: @productField()
            platform: @selectedPlatform().name
            quantity: @quantityField()
            price: @priceField()
            side: @selectedAction()
            exchange: 0
            cost: 0
          })
          contentType: "application/json"
        }).done(() =>
          $("#saveTransactionModal").modal("hide")
          @showInfo("Transaction created for " + @productField())
        ).fail((req, status, err) =>
          @showError(req.responseText)
        ).always(() =>
          @productField(null)
          @quantityField(null)
          @priceField(null)  
        )

      @deleteTransactionDialog = () =>
        if @selectedTransaction() != null
          @showModal("Delete Transaction?", "Really delete the selected transaction?", "Delete", @deleteTransaction)  
        else
          @showError("Please select a transaction first")  

      @deleteTransaction = () =>
        @ajax(routes.controllers.TransactionController.deleteTransaction(), {
          data: JSON.stringify(@selectedTransaction())
          contentType: "application/json"
        }).done(() =>
          @showInfo("Transaction deleted for " + @selectedTransaction().product)
        ).fail((req, status, err) =>
          @showError(req.responseText)
        ).always(() =>
          @selectedTransaction(null)
        )  

      @selectTransaction = (transaction) =>
        @selectedTransaction(transaction)

      @isSelectedTransaction = (transaction)=>
        @isSame(transaction, @selectedTransaction())  

      @getTransactions = () -> 
        getTransactions("")
        
      @getTransactions = (code) ->
        @ajax(routes.controllers.TransactionController.getTransactions(code, 0, rowsPerPage))
          .done((data, status, xhr) =>
            @loadTransactions(data, status, xhr)
          )
          .fail((req, status, err) =>
            @showError(req.responseText)
          )

      @nextTransactions = () ->
        if @nextTransactionsUrl()
          $.ajax({url: @nextTransactionsUrl()}).done((data, status, xhr) =>
            @loadTransactions(data, status, xhr)
          )

      @prevTransactions = () ->
        if @prevTransactionsUrl()
          $.ajax({url: @prevTransactionsUrl()}).done((data, status, xhr) =>
            @loadTransactions(data, status, xhr)
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

      @saveProduct = () =>
        @ajax(routes.controllers.ProductController.saveProduct(), {
          data: JSON.stringify({
            code: @productField()
            description: @productDescriptionField()
          })
          contentType: "application/json"
        }).done(() =>
          $("#saveProductModal").modal("hide")
          @showInfo("Product created: " + @productField())
        ).fail((req, status, err) =>
          @showError(req.responseText)
        ).always(() =>
          @productField(null)
          @productDescriptionField(null)
        )

      @deleteProductDialog = () =>
        if @selectedProduct() != null
          @showModal("Delete Product?", "Really delete the selected product?", "Delete", @deleteProduct)  
        else
          @showError("Please select a product first")


      @deleteProduct = () =>
        @ajax(routes.controllers.ProductController.deleteProduct(), {
          data: JSON.stringify(@selectedProduct())
          contentType: "application/json"
        }).done(() =>
          @showInfo("Product deleted: " + @selectedProduct().code)
        ).fail((req, status, err) =>
          @showError(req.responseText)
        ).always(() =>
          @resetProduct()
        )

      @selectProduct = (product) =>
        @selectedProduct(product)
        @getTransactions(product.code)

      @resetProduct = () =>
        @selectedProduct(undefined)
        @getTransactions()

      @isSelectedProduct = (product)=>
        @isSame(product, @selectedProduct())

      @getProducts = () =>
        @ajax(routes.controllers.ProductController.getProducts(0, rowsPerPage))
          .done((data, status, xhr) =>
            @loadProducts(data, status, xhr)
          ).fail((req, status, err) =>
            @showError(req.responseText)
          )

      @nextProducts = () =>
        if @nextProductsUrl()
          $.ajax({url: @nextProductsUrl()}).done((data, status, xhr) =>
            @loadProducts(data, status, xhr)
          )

      @prevProducts = () =>
        if @prevProductsUrl()
          $.ajax({url: @prevProductsUrl()}).done((data, status, xhr) =>
            @loadProducts(data, status, xhr)
          )  

      #
      # Platforms
      #

      @platforms = ko.observableArray()
      @platformField = ko.observable()      
      @currencyField = ko.observable() 
      @selectedPlatform = ko.observable()

      @savePlatform = () =>
        @ajax(routes.controllers.PlatformController.savePlatform(), {
          data: JSON.stringify({
            name: @platformField()
            currency: @currencyField()
          })
          contentType: "application/json"
        }).done(() =>
          $("#savePlatformModal").modal("hide")
          @showInfo("Platform created: " + @platformField())
        ).fail((req, status, err) =>
          @showError(req.responseText)
        ).always(() =>
          @platformField(null)
          @currencyField(null)
        )

      @deletePlatformDialog = () =>
        if @selectedPlatform() != null
          @showModal("Delete Platform?", "Really delete the selected platform?", "Delete", @deleteProduct)  
        else
          @showError("Please select a platform first")

      @deletePlatform = () =>
        @ajax(routes.controllers.PlatformController.deletePlatform(), {
          data: JSON.stringify(@selectedPlatform())
          contentType: "application/json"
        }).done(() =>
          @showInfo("Platform deleted: " + @selectedPlatform().name)
        ).fail((req, status, err) =>
          @showError(req.responseText)
        ).always(() =>
          @selectedPlatform(null)
        )

      @selectPlatform = (platform) =>
        @selectedPlatform(platform)

      @isSelectedPlatform = (platform)=>
        @isSame(platform, @selectedPlatform())

      @getPlatforms = () =>
        @ajax(routes.controllers.PlatformController.getPlatforms())
          .done((data, status, xhr) =>
            @loadPlatforms(data, status, xhr)
          ).fail((req, status, err) =>
            @showError(req.responseText)
          )

      @load()    

    log: (msg) ->
      setTimeout(() -> 
        throw new Error(msg)
      , 0)

    load: () ->  
      @getPlatforms()
      @getProducts()
      @getTransactions()

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

    loadPlatforms: (data, status, xhr) ->
      @platforms(data)    
      @selectPlatform(data)

    onTransactionCreated: (transaction) ->  
      if @prevTransactionsUrl() == null
        @transactions.unshift(transaction)
      
      if @transactions().length > @rowsPerPage
        @transactions.pop()

    onTransactionDeleted: (transaction) ->  
      @transactions.remove((t) => @isSame(t, transaction))

    onProductCreated: (product) ->
      if @prevTransactionsUrl() == null
        @products.unshift(product)
      
      if @products().length > @rowsPerPage
        @products.pop()

    onProductDeleted: (product) ->
      @products.remove((p) => @isSame(p, product))

    onPlatformCreated: (platform) ->
      @platforms.unshift(platform)

    onPlatformDeleted: (platform) ->
      @platforms.remove((p) => @isSame(p, platform))
)