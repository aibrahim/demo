(ns app.api
  (:require [reitit.coercion.spec]
            [spec-tools.data-spec :as ds]
            [reitit.swagger :as swagger]
            [app.persistence :as p]))

(defn gen-exception
  "Generate exception response"
  [e status-code]
  {:status status-code
   :body   (ex-data e)})

(defn create-user-handler
  "A web handler to create a new user"
  [{{user :body} :parameters}]
  (try
    {:status 201
     :body (p/create-user! p/user-database user)}
    (catch Exception e (gen-exception e 400))))

(defn authenticate-user-handler
  "A web handler to authenticate an existing user"
  [{{{:keys [username password]} :query} :parameters}]
  (try
    {:status 200
     :body   (p/authenticate-user p/user-database username password)}
    (catch Exception e (gen-exception e 401))))

(def routes
  ["" {:coercion reitit.coercion.spec/coercion}
   ["/swagger.json"
    {:get {:no-doc true
           :swagger {:info {:title "Authentication API"
                            :description "Simple Authentication API."}}
           :handler (swagger/create-swagger-handler)}}]
   ["/user"
    {:swagger {:tags ["User"]}}
    [""
     {:get  {:summary    "Authenticates a user in the database, returning their details"
             :parameters {:query {:username string?
                                  :password string?}}
             :handler    authenticate-user-handler}
      :post {:summary    "Creates a new user in the database, returning their details"
             :parameters {:body {:username      string?
                                 :password      string?
                                 (ds/opt :role) keyword?}}
             :handler    create-user-handler}}]]])
