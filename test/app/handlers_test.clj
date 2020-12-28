(ns app.handlers-test
  (:require [clojure.test :refer :all]
            [app.api :refer :all]
            [ring.mock.request :as mock]
            [clojure.data.json :as json]
            [app.server :refer [app]]))

(defn read-body
  "Read reposnse body."
  [body]
  (-> body
      slurp
      (json/read-str :key-fn keyword)))

(deftest test-create-user
  (testing "Test create-user"
    (testing "Password is short"
      (let [{:keys [status body]} (app
                                   (-> (mock/request :post "/user" (json/write-str {:username "user1" :password "12aA#"}))
                                       (mock/content-type "application/json")))
            violation (-> body read-body :violations first)]
        (is (= status 400))
        (is (= violation "password.error/too-short"))))

    (testing "Password is not containing special char"
      (let [{:keys [status body]} (app
                                   (-> (mock/request :post "/user" (json/write-str {:username "user2" :password "helloWorld01"}))
                                       (mock/content-type "application/json")))
            violation (-> body read-body :violations first)]
        (is (= status 400))
        (is (= violation "password.error/missing-special-character"))))

    (testing "Password is not containing lowercase char"
      (let [{:keys [status body]} (app
                                   (-> (mock/request :post "/user" (json/write-str {:username "user3" :password "HELLOWORLD#12"}))
                                       (mock/content-type "application/json")))
            violation (-> body read-body :violations first)]
        (is (= status 400))
        (is (= violation "password.error/missing-lowercase"))))

    (testing "Password is not containing uppercase char"
      (let [{:keys [status body]} (app
                                   (-> (mock/request :post "/user" (json/write-str {:username "user4" :password "helloworld#12"}))
                                       (mock/content-type "application/json")))
            violation (-> body read-body :violations first)]
        (is (= status 400))
        (is (= violation "password.error/missing-uppercase"))))

    (testing "Valid user with default user role"
      (let [{:keys [status body]} (app
                                   (-> (mock/request :post "/user" (json/write-str {:username "user5" :password "helloWorld#12"}))
                                       (mock/content-type "application/json")))
            response (-> body read-body)]
        (is (= status 201))
        (is (= response {:id "user5", :role "user"}))))

    (testing "Valid user with custom role"
      (let [{:keys [status body]} (app
                                   (-> (mock/request :post "/user" (json/write-str {:username "user6" :password "helloWorld#12" :role "admin"}))
                                       (mock/content-type "application/json")))
            response (-> body read-body)]
        (is (= status 201))
        (is (= response {:id "user6", :role "admin"}))))

    (testing "Already exists user"
      (let [{:keys [status body]} (app
                                   (-> (mock/request :post "/user" (json/write-str {:username "user5" :password "helloWorld#12"}))
                                       (mock/content-type "application/json")))
            reason (-> body read-body :reason)]
        (is (= status 400))
        (is (= reason "create-user.error/already-exists"))))))

(deftest test-authenticate-user
  (testing "Test authenticate-user"
    (testing "not exists user"
      (let [{:keys [status body]} (app
                                   (-> (mock/request :get "/user" {:username "user100" :password "helloWorld#123"})
                                       (mock/content-type "application/json")))
            reason (-> body read-body :reason)]
        (is (= status 401))
        (is (= reason "login.error/invalid-credentials"))))

    (testing "with invalid credentials"
      (let [{:keys [status body]} (app
                                   (-> (mock/request :get "/user" {:username "user5" :password "helloWorld#123"})
                                       (mock/content-type "application/json")))
            reason (-> body read-body :reason)]
        (is (= status 401))
        (is (= reason "login.error/invalid-credentials"))))

    (testing "with valid credentials"
      (let [{:keys [status body]} (app
                                   (-> (mock/request :get "/user" {:username "user5" :password "helloWorld#12"})
                                       (mock/content-type "application/json")))
            response (-> body read-body)]
        (is (= status 200))
        (is (= response {:id "user5" :role "user"}))))))


