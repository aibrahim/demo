(ns app.server
  (:require [ring.adapter.jetty :as jetty]
            [ring.middleware.params :as params]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [muuntaja.core :as m]
            [reitit.ring.coercion :as coercion]
            [reitit.ring :as ring]
            [reitit.swagger-ui :as swagger-ui]
            [reitit.swagger :as swagger]
            [ring.middleware.cors :refer [wrap-cors]]
            [app.api]))

(def default-middlewares [swagger/swagger-feature
                          params/wrap-params
                          muuntaja/format-middleware
                          coercion/coerce-exceptions-middleware
                          coercion/coerce-request-middleware
                          coercion/coerce-response-middleware])

(def app
  (ring/ring-handler
   (ring/router
    [app.api/routes]
    {:data {:muuntaja m/instance
            :middleware default-middlewares}})
   (ring/routes
    (swagger-ui/create-swagger-ui-handler
     {:path "/"
      :config {:validatorUrl nil
               :operationsSorter "alpha"}})
    (ring/create-default-handler))
   {:middleware [[wrap-cors
                  :access-control-allow-origin [#"http://localhost:8080"]
                  :access-control-allow-methods [:get :post]]]}))

(defn -main [& args]
  (jetty/run-jetty #'app {:port 3000, :join? false})
  (println "Server running on port 3000"))

(comment
  (-main))
