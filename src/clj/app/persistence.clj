(ns app.persistence)

(def default-users-records {"defaultuser" {:id       "defaultuser"
                                           ;; default password must not violate the password rules.
                                           :password "Password@123"
                                           :role     :reader}})

;; Persistence interface
(defprotocol Persistence
  (create-user! [this user]
   "Create a user by adding them to the database, and returns the user's details except for their password")
  (authenticate-user [this username password]
   "Returns a user map when a username and password are correct, or nil when incorrect")
  (init-users! [this users]
   "Initialize users into database"))

(defn password-rules
  "Given a password, return a set of keywords for any rules that are not satisfied"
  [password]
  (cond-> #{}
    ; password must be at least 8 characters long
    (<= (count password) 8)
    (conj :password.error/too-short)

    ; password must contain a special character
    (empty? (re-find #"[!@#$%^&*(),.?\":{}|<>]" password))
    (conj :password.error/missing-special-character)

    ; password contains at least one lower case letter
    (not-any? #(Character/isLowerCase %) password)
    (conj :password.error/missing-lowercase)

    ; password contains at least one upper case letter
    (not-any? #(Character/isUpperCase %) password)
    (conj :password.error/missing-uppercase)))

(defn authenticate-user* [db-atom username password]
  (if-let [{known-password :password :as user} (get @db-atom username)]
    (if (= password known-password)
      (dissoc user :password)
      (throw (ex-info "Invalid username or password"
                      {:reason :login.error/invalid-credentials})))
    ;; do not return nil.
    (throw (ex-info "Invalid username or password"
                    {:reason :login.error/invalid-credentials}))))

(defn create-user*
  ([db-atom username password]
   ; recur with a default role of :user
   (create-user* db-atom username password :user))
  ([db-atom username password role]
   ;; serious note: 
   ;; we need to separate password checking functionality from here, and
   ;; depending on checking them using specs in requests.

   ; if there are any password violations
   (if-let [password-violations (not-empty (password-rules password))]
     ; then throw an exception with the violation codes
     (throw (ex-info "Password does not meet criteria"
                     {:reason     :create-user.error/password-violations
                      :violations password-violations}))
     ; otherwise check if the user exists
     (if (get @db-atom username)
       ; and if they do then return an error code
       (throw (ex-info "User already exists"
                       {:reason :create-user.error/already-exists}))
       ; otherwise return a success message with the user's details
       (-> db-atom
           ; put the user in the database
           (swap! assoc username {:id username :role role :password password})
           ; select the user in the database
           (get username)
           ; strip their password
           (dissoc :password))))))

(defn gen-memory-db
  "Generate in-memory database"
  []
  (let [db (atom {})]
    (reify Persistence
      (create-user! [_ {:keys [username password role]}] (if role
                                                           (create-user* db username password role)
                                                           (create-user* db username password)))
      (authenticate-user [_ username password] (authenticate-user* db username password))
      (init-users! [_ users] (reset! db users)))))

; in memory database for users
(def user-database (gen-memory-db))

;; publish default users into in-memory database
(init-users! user-database default-users-records)


