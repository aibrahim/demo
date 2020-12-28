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
                                 ;; password should be checked here not inside create-user functionality,
                                 ;;  example spec to match password-rules fucntion:
                                 ;; (def special-chars-regexp #"[!@#$%^&*(),.?\":{}|<>]")
                                 ;; (s/def :password/valid-length?
                                 ;; (st/spec {:spec (s/and string? #(>= (count %) 8))
                                 ;;                                :reason "password must be at least 8 characters long."}))

                                 ;; (s/def :password/has-special-chars?
                                 ;;  (st/spec {:spec (s/and string?
                                 ;;                         #((complement str/blank?) (re-find special-chars-regexp %)))
                                 ;;            :reason "password must contain a special character."}))

                                 ;; (s/def :password/has-lowercase-chars?
                                 ;;  (st/spec {:spec (s/and string?
                                 ;;                         (fn [x] (some #(Character/isLowerCase %) x)))
                                 ;;            :reason "password must contain at least one lower case letter."}))

                                 ;;  (s/def :password/has-uppercase-chars?
                                 ;;  (st/spec {:spec (s/and string?
                                 ;;                         (fn [x] (some #(Character/isUpperCase %) x)))
                                 ;;            :reason "password must contain at least one upper case letter."}))

                                 ;; (s/def ::password (st/spec (s/and :password/valid-length?
                                 ;;                                  :password/has-special-chars?
                                 ;;                                  :password/has-lowercase-chars?
                                 ;;                                  :password/has-uppercase-chars?)))
                                 ;;                                  
                                 :password      string?
                                 (ds/opt :role) keyword?}}
             :handler    create-user-handler}}]]])
